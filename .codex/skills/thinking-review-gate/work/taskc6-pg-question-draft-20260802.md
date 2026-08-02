결론부터 말하면, 질문한 의문이 맞다. 이 문장은 “외부 PG라서 인수테스트로 검증할 수 없다”는 뜻이 아니다. 정확한 뜻은 “현재 만든 Cucumber 인수테스트 구성에서는 결제 HTTP 어댑터를 테스트 더블로 갈아 끼웠기 때문에 그 어댑터의 요청 경로와 응답 해석을 검증하지 않는다”이다.

## 알기 쉬운 비유

배달 주문 전체를 시험하면서 배달 기사 자리에 테스트용 직원을 세운 것과 같다. 주문 접수, 조리, 배달 실패 처리까지는 확인할 수 있지만 실제 배달 앱의 주소 형식이 맞는지는 확인할 수 없다. 테스트용 직원은 실제 앱을 호출하지 않기 때문이다.

## 정의와 현재 구성

현재 `protocol` 테스트는 실제 HTTP 요청으로 애플리케이션에 들어가 실제 MySQL까지 간다. 그러나 결제 포트에는 `@Primary`인 `TestChargePort`가 주입된다. 따라서 다음은 검증한다.

- 결제 포트가 `false`를 반환하면 예매가 실패한다.
- HTTP 응답이 402와 `problem+json`으로 변환된다.
- 티켓이 예약되지 않는다.

반면 다음은 검증하지 않는다.

- `PgChargeAdapter`가 `POST /charge`를 호출하는가.
- 요청 본문이 `{paymentInfo, amount}`인가.
- HTTP 200의 본문 `approved:false`를 거절로 해석하는가.

실제 어댑터가 아예 실행되지 않으므로 `/payments`와 2xx 승인이라는 틀린 구현도 통과했다.

## 해야 하는가

두 층으로 답해야 한다.

첫째, C-6 과제의 최소 요구에는 현재 구성이 들어맞을 가능성이 높다. 과제는 walking skeleton을 HTTP부터 Testcontainers DB까지 명시했고 외부 PG까지 실물로 관통하라고 하지는 않았다. 외부 의존성을 대역으로 두는 것도 일반적인 테스트 선택이다.

둘째, L1 아웃바운드 어댑터를 생성하고 그 층을 게이트로 판정한다는 파이프라인의 주장에는 빈틈이 있다. L1이 결제 어댑터도 생성하는데 그 코드가 어느 자동 테스트에서도 실행되지 않는다면 L1 전체를 검증했다고 말할 수 없다. 계약을 프롬프트에 넣고 수동 스모크를 한 것은 이번 코드를 바로잡았지만, 다음 실행에서의 회귀를 막는 자동 판정은 아니다.

추천은 Cucumber 전체 인수테스트를 반드시 무겁게 만드는 것이 아니라, L1 게이트에 결제 어댑터 전용 통합 또는 계약 테스트를 추가하는 것이다. WireMock 같은 로컬 PG 대역을 실제 HTTP로 호출해 적어도 승인과 거절 두 경우를 검증한다. 특히 거절 테스트는 HTTP 200과 `{approved:false}`를 반환하게 해야 한다. 그러면 잘못된 `/payments`는 요청 불일치로 실패하고, 2xx 승인 판정도 거절 시나리오에서 실패한다.

선택지는 다음과 같다.

- 어댑터 통합 테스트를 L1 게이트에 추가한다. 빠르고 실패 원인이 명확해 가장 적합하다.
- 기존 Cucumber `protocol` 구성에서 결제 더블을 제거하고 WireMock을 붙인다. 전체 왕복을 증명하지만 테스트가 느리고 외부 프로토콜 결함과 업무 결함의 원인 분리가 어려워진다.
- 지금처럼 수동 스모크만 유지한다. 과제 최소 범위에는 충분할 수 있지만 반복 가능한 게이트가 아니므로 파이프라인 신뢰도는 낮다.

## 외부 PG라서 못 하는가

아니다. 프로덕션 PG를 실제 결제와 함께 호출할 필요는 없다. 알려진 계약을 기준으로 로컬 스텁이나 PG 샌드박스를 호출하면 된다. 다만 로컬 스텁은 “우리 코드가 우리가 적어 둔 계약을 지키는가”만 증명한다. 그 스텁 자체가 실제 PG 계약과 같은지는 제공자 공식 문서, 스키마, 샌드박스 또는 provider 측 계약 테스트로 별도 확인해야 한다.

따라서 문제의 문구는 다음처럼 바꾸는 편이 정확하다.

> 현재 Cucumber 인수테스트는 `ChargePort`를 테스트 더블로 대체하므로 `PgChargeAdapter`의 HTTP 계약은 검증하지 않는다. 이 계약을 추측하지 말고 제공된 계약을 구현하라. L1 판정에서는 별도의 어댑터 계약 테스트로 검증한다.

아직 별도 자동 테스트를 추가하지 않을 것이라면 마지막 문장은 “docker compose 스모크로 별도 확인한다”가 정직하다. 다만 이 경우 `notes` 의존 문제는 완전히 해결된 것이 아니다.

## 확인 근거

- 과제 원문: `task3/assignments/taskC-6.md`
- 테스트 구성: `task3/ticket-reservation-c6/src/test/java/com/thinking/ticket/e2e/E2eCucumberSpringConfiguration.java`
- 결제 더블: `task3/ticket-reservation-c6/src/test/java/com/thinking/ticket/jpa/TestPaymentConfig.java`
- 실행 구성: `.codex/skills/skeleton-agent/pipeline/inputs/c6-ticket-skeleton.json`
- 스텁 계약: `task3/ticket-reservation-c6/docker/mock-pg/mappings/charge-declined.json`
