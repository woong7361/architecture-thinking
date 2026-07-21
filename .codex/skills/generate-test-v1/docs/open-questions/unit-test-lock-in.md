# 열린 질문 — unit 테스트를 사전 생성하면 구현을 잠그는가

> 상태: **열림(토론 중)**. generate-test 하네스의 산출물 초안을 두고 나눈 토론 기록.
> 자매 문서: [feature-altitude-coupling.md](feature-altitude-coupling.md).

## 질문

이 하네스는 **구현이 있기 전에** 정책만으로 unit 테스트를 생성한다. 그런데 생성된 테스트가
정책이 요구하지 않은 **구현 우연(null 처리 방식, 기본값, 호출 순서 등)** 까지 단언해버리면,
그 테스트는 정책이 아니라 **특정 구현을 잠그는 족쇄**가 된다. 나중에 정당한 리팩터링·다른 설계가
"정책은 그대로인데 테스트만 빨강"이 되어 막힌다. **사전 생성 unit 테스트는 이 위험이 특히 크다** —
TDD에서 사람이 설계하며 쓰는 테스트와 달리, 하네스는 구현을 상상해서 단언을 채우기 때문이다.

## 왜 지금 이게 걸리나 — 하네스가 실제로 뽑은 테스트

`runs/unit-standalone/ticket-char/2026-07-13_57796ebc/artifact/TicketServiceTest.java`
(정책: 유저 존재 + 미예약 티켓 + 결제 성공 시 예약). PASS(4.x)로 승격된 산출물이다.

의심 단언들:

1. **NPE를 기대 동작으로 박제** — `reserveTicket_throwsNullPointerException_whenTicketRepositoryReturnsNull`:
   티켓 조회가 null이면 **NullPointerException**을 던진다고 단언(`.isInstanceOf(NullPointerException.class)`).
   이건 정책("미예약 티켓이면 예약")이 아니라 **null 체크가 없는 현재 구현의 우연**을 고정한다.
   방어 코드를 추가해 `TicketNotFoundException`을 던지도록 고치면 정책은 그대로인데 이 테스트가 깨진다.

2. **기본값 구현 세부에 의존** — 결제 실패 시 `assertThat(ticket.getUserId()).isEqualTo(0L)`:
   "예약이 안 됐다"는 정책적 사실을 `userId == 0L`이라는 **원시 기본값**으로 검증. userId 표현을
   Optional/null/-1로 바꾸면 정책 불변인데 테스트가 깨진다.

3. **호출 순서·상호작용 과다 단언** — `InOrder`(charge → save), `verifyNoMoreInteractions`,
   `verify(...).never()` 다수. "결제 없이 예약 저장 금지"는 정책일 수 있으나, **charge가 save보다
   먼저**라는 순서 단언은 구현 결합이다. 다른 트랜잭션 설계(저장 예약 후 결제 확정)를 원천 차단한다.

→ Gen 프롬프트는 "정책·원문에 근거 없는 값 금지", "순수 로직은 실제 객체 상태 검증"을 명시하는데도
   위 세 개가 **게이트를 통과해 최종 산출물로 승격**됐다. 즉 현행 rubric·가드는 이 lock-in을 못 잡는다.

## 쟁점

1. **정책 vs 구현의 경계를 테스트에서 어떻게 긋나?**
   - "결제 성공해야 예약된다" = 정책. "userId가 0L로 남는다" = 구현. 둘을 가르는 결정적 기준이 필요.
   - 상태 검증(예약 여부)은 정책적, 상호작용 검증(호출 순서·횟수)은 구현적 — 이 원칙을 rubric에 넣을까?
2. **사전 생성 자체가 문제인가, 단언 선택이 문제인가?**
   - (a) "사전 생성이 근본 문제": 구현 전 unit은 필연적으로 구현을 상상 → 잠금. 계약(정책 고도)만
     사전 생성하고 unit은 구현과 함께 써야 한다는 입장.
   - (b) "단언 선택의 문제": 사전 생성이라도 **관찰 가능한 정책 결과만** 단언하면 안전. Gen을 그쪽으로
     제약하면 lock-in 없이 사전 생성 가치를 유지.
3. **NPE 같은 단언은 봉인 테스트 방지("빨강 가능성")를 만족하는데도 왜 나쁜가?**
   - "빨강 가능성"(틀린 구현이면 실패)과 "정당한 리팩터링에도 빨강"(과잉명세)은 다르다.
     현행 rubric의 unambiguity는 전자만 본다 — **후자(과잉명세)를 잡는 축이 없다.**
4. **mock_discipline로 일부 잡히나?** verify 과용은 mock_discipline 캡으로 눌리지만,
   NPE·0L 같은 **상태 단언의 과잉명세**는 그 축의 사정거리 밖이다.

## 후보 방향 (아직 결정 아님)

- **D1. rubric에 "과잉명세/구현결합" 축 추가**: "정당한 리팩터링에도 깨지는 단언 개수"를 감점.
  상호작용 단언(InOrder·verifyNoMoreInteractions), 원시 기본값 의존, 미명세 예외 타입 단언을 신호로.
- **D2. Gen 프롬프트에 "정책 결과만 단언" 규율 추가**: null/기본값/호출순서를 정책이 명시하지
  않으면 단언 금지. 예외는 정책이 정의한 도메인 예외 타입만(NPE·IllegalState 같은 언어 기본 예외 금지).
- **D3. unit은 사전 생성 안 함(계약만 사전)**: split에서 contract만 사전 동결하고, unit은 구현
  단계에서 사람이 소유. 하네스는 unit "초안 힌트"만 제공하고 게이트 승격은 안 함.
- **D4. 결정적 가드**: `assertThatThrownBy(...).isInstanceOf(NullPointerException/RuntimeException...)`
  등 **언어 기본 예외 단언**과 `getX()).isEqualTo(0/0L/null)` 패턴을 forbidden 가드로.

## 토론 로그

<!-- (YYYY-MM-DD) 발언 요약 / 합의·보류 사항을 여기 누적 -->
- (2026-07-21) 문서 개설. TicketServiceTest의 세 lock-in 단언과 쟁점 정리. 방향 미결정.
- (2026-07-21) 방향 확정(→ [../v1-spec-first-design.md](../v1-spec-first-design.md)): 단위를 A/B로 분리 —
  **A(도메인 규칙)는 사전 "예시표 문서"로 생성, B(mock·순서·예외 타입 = lock-in)는 사후 특성화 스킬로 분리.**
  사전 일괄 단위 코드 생성은 폐기. 이 문서는 **열림 유지** — 예시표 문서화가 실제로 lock-in을 걸러내는지
  `generate-test-v1`에서 실증 후 closed 이관.
