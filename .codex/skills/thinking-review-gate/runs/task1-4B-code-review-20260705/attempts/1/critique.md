## 문제 지점

- [Finding 1 / 테스트 구조] 초안의 “B에는 `src/test` 트리가 없다”는 근거는 현재 워크트리 기준으로는 맞지 않을 수 있다. `task1/task1-4-history-B/src/test/resources/features/refund.feature`, `CucumberAcceptanceTest.java`, `RefundFeatureStepDefinitions.java`가 존재한다면, 핵심 문제는 “테스트가 없다”가 아니라 “B의 자체 Cucumber 게이트가 A의 인수테스트와 동일한 판정력을 갖는지, 그리고 Maven으로 실제 실행되는지”로 바뀐다.

- [Finding 1 / API 불일치] `PaymentPlatform`, `RefundProcessor`, `RefundRequest`, reason enum 등이 없다는 지적은 A 인수테스트를 그대로 재사용할 때의 호환성 문제로는 타당하지만, `refund_design.md`의 task1 단위테스트 범위는 외부 결제 의존을 제외한다고 되어 있다. 따라서 이 항목은 “도메인 설계 위반”이 아니라 “과제의 Feature/Step 게이트와 public API가 맞지 않음”으로 분리해서 써야 한다.

- [수동 환불 coupling] “manual refund request cannot be represented”는 과하게 단정되어 있다. `RefundPolicy.MANUAL.calculate(long manualAmount, long cancellableAmount)` 경로는 proration 입력 없이 수동 환불을 표현할 수 있다. 더 정확한 문제는 `RefundCalculationRequest` 또는 static priority-chain API를 사용할 때 `totalDays`, `remainingDays`가 manual 우선순위보다 먼저 검증되어, 수동 환불인데도 무관한 일할 계산 입력 때문에 실패할 수 있다는 점이다.

- [검증 결과 표현] Maven/Cucumber 게이트는 `JAVA_HOME` 문제로 실행되지 않았으므로, “likely fail at compile/glue level”은 추정으로 표시해야 한다. 현재 B에 자체 test runner/step이 존재한다면 실제 실패 지점은 A step API 컴파일 문제가 아니라 B step의 동작/판정력/컴파일 여부일 수 있다.

- [누락된 검토 축] 초안은 B 자체 step glue가 있는 경우 그 step이 production API 역할을 대신 수행하는 문제를 다루지 않는다. 과제의 핵심이 “AI가 새로 짠 구현을 내가 쓴 Feature로 판정”하는 것이라면, 테스트 glue가 환불 흐름을 직접 조립하면서 예외를 넓게 허용하는지 확인해야 한다. 이는 acceptance test가 실제 구현을 엄격히 판정하는지에 직접 영향을 준다.

- [대안과 trade-off] Recommendation에는 adapter 추가와 Feature/Step 복사·수정이라는 선택지가 보이지만 trade-off가 부족하다. adapter는 A 게이트 재사용성을 높이지만 코드가 늘고 exception mapping이 필요하다. B 전용 step을 쓰는 방식은 빠르지만 A acceptance gate와 동일한 신뢰도를 주장하기 어렵다.

## 확인 필요

- 현재 최종 리뷰 대상 시점에 `task1/task1-4-history-B/src/test`가 실제로 존재하는지, 그리고 `./mvnw.cmd test`가 Java 17 환경에서 실행되는지 확인해야 한다.
- B 자체 Cucumber step이 있다면 A의 Feature/Step과 시나리오, 예외 reason, public API 기대치가 동일한지 비교해야 한다.
- 앱스토어/구글 플레이 결제 제한은 `refund_design.md` 범위 밖으로 볼지, 과제 acceptance gate의 일부로 볼지 명확히 해야 한다.
- manual refund 문제는 어떤 public API를 과제의 대표 진입점으로 볼지 정해야 한다: enum overload, `RefundCalculationRequest`, static `calculateRefundAmount`, 또는 별도 application service.

## 수정 제안

- Finding 1을 “테스트 없음”으로 단정하지 말고, “A acceptance gate와 B public API/테스트 게이트의 불일치”로 재작성한다. 현재 B에 자체 테스트가 있으면 그 존재를 인정한 뒤, 동일 게이트인지 별도 완화 게이트인지 검토하도록 바꾼다.
- manual refund finding은 API 경로별로 좁혀 쓴다. 예: “manual-specific overload는 가능하지만, request-object 기반 priority-chain API는 manual 우선순위 전에 proration 필드를 검증한다.”
- `JAVA_HOME` 때문에 Maven 검증이 중단된 부분과 그로 인해 compile/glue 실패가 미확인이라는 한계를 명확히 표시한다.
- Recommendation에 우선순위를 둔다: 먼저 Java 17로 B Cucumber 게이트 실행, 그다음 A Feature/Step과 동일성 확인, 이후 adapter 또는 public API 정리 선택.
- 설계 범위와 acceptance gate 범위를 분리해서 적는다. 외부 결제 플랫폼 관련 타입 부재는 설계 위반으로 단정하지 말고, A acceptance test 재사용 조건에서 필요한 adapter gap으로 표현한다.

## 요약

초안은 핵심 리스크인 “A 인수테스트로 B를 그대로 판정하기 어렵다”는 방향은 잘 잡았지만, 테스트 구조와 manual API에 대한 일부 표현이 과하게 단정적이다. 현재 B의 실제 test tree와 실행 결과를 다시 확인하고, 설계 위반과 acceptance gate 호환성 문제를 분리하면 더 검증 가능한 코드 리뷰가 된다.