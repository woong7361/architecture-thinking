## 문제 지점

- [Finding 1 / 약해진 인수테스트] “기존 Feature도 이를 검증했습니다”라는 핵심 근거가 현재 초안 안에서는 사용자가 따라갈 수 있을 만큼 고정되어 있지 않습니다. `task1/task1-4-history-A/src/test/resources/features/refund.feature` 또는 이전 `task1/src/test/resources/features/refund.feature`의 구체 시나리오/라인을 붙이거나, 현재 근거가 `input.md`의 checked context에 의존한다는 점을 명시해야 합니다.
- [Finding 1 / 과제 목적 연결] “과제 목적이 흔들립니다”는 판단은 타당해 보이지만, 왜 “B 구현 코드 자체의 결함”이 아니라 “게이트 품질 결함”인지 조금 더 분리해야 합니다. 현재 문장만 보면 구현 버그와 테스트 회귀가 섞여 보일 수 있습니다.
- [Finding 2 / 실패 가능성] 앱스토어/구글 플레이 시나리오가 실패할 가능성이 크다는 정적 판단은 근거가 있지만, 초안은 실제 Maven 테스트가 실행되지 않았다는 한계를 뒤에서만 언급합니다. 해당 Finding 안에서도 “정적 코드 흐름상”이라는 조건을 더 앞에 두면 단정으로 보이는 위험이 줄어듭니다.
- [Finding 2 / 테스트 코드 문제 vs 도메인 문제] 이 항목은 실제 제품 코드의 환불 정책 결함이라기보다 Feature/Step의 기대와 구현 흐름이 맞지 않는 문제입니다. 코드 리뷰 결과로는 중요하지만, “도메인 구현이 외부 스토어 환불을 막아야 하는 설계인지”와 “테스트 Step이 임의로 기대하는 것인지”를 구분해야 합니다.
- [Finding 3 / 환불 상태 전이] `refund.succeed()`를 호출하지 않는 문제는 설계 Happy Path와 acceptance flow의 불일치로 잘 짚었지만, 도메인 클래스 자체에는 상태 전이 메서드와 terminal guard가 있다는 점을 Finding 안에서 더 명확히 구분하면 좋습니다. 지금 표현은 도메인 구현 누락인지 테스트/사용 흐름 누락인지 약간 모호합니다.
- [Finding 4 / request-object 기반 정책 API] “static priority-chain API를 쓰면”이라는 claim은 현재 초안에서 충분히 따라갈 수 있는 근거가 부족합니다. `RefundPolicy`의 해당 static API 라인, `RefundCalculationRequest` 생성 시점, manual 우선순위가 실제로 무력화되는 최소 예시를 제시해야 합니다. 실제 호출 경로가 없다면 severity를 낮추거나 “잠재 API 설계 문제”로 표현하는 편이 안전합니다.
- [Finding 5 / Order 생성자 invariant] `PARTIALLY_REFUNDED`인데 `canceledAmount == 0` 같은 상태가 항상 도메인상 금지되어야 하는지는 설계 근거가 초안에 직접 제시되어 있지 않습니다. 설계 문서가 상태와 누적 환불액 정합성을 요구하는지 확인하거나, “재수화/fixture에서 생길 수 있는 방어적 invariant 후보” 정도로 낮춰 표현해야 합니다.
- [Recommendation / 선택지] 추천은 A Feature를 B에 복사하는 쪽으로 명확하지만, B 전용 Feature를 유지하는 대안의 trade-off가 짧습니다. “왜 빠른지”, “어떤 보강을 해야 동등한 게이트가 되는지”를 한 문장만 더 붙이면 선택 판단이 좋아집니다.

## 확인 필요

- 기존 A 또는 이전 Feature에서 `구독 만료 마지막 날 환불`, `일할 단가 소수점 절사` 시나리오가 있는 정확한 파일/라인.
- 외부 스토어 결제는 도메인 설계상 반드시 거절해야 하는 요구사항인지, 아니면 현재 B Feature/Step에만 추가된 테스트 기대인지.
- `RefundPolicy.MANUAL.calculate(RefundCalculationRequest)` 또는 static priority-chain API가 실제 코드에서 호출되는 경로가 있는지.
- `Order` 생성자의 상태와 `canceledAmount` 정합성이 설계 문서의 명시 요구인지, 아니면 리뷰어가 제안하는 추가 invariant인지.

## 수정 제안

- 각 Finding 제목에 “테스트 게이트 문제”, “acceptance step 불일치”, “도메인 API 설계 위험”처럼 결함의 층위를 표시해 구현 결함과 테스트 결함을 분리하세요.
- Finding 1에는 기존 Feature의 정확한 evidence anchor를 추가하고, “현재 구현은 처리하지만 B 인수테스트만으로는 회귀를 잡지 못한다”는 결론을 더 직접적으로 쓰세요.
- Finding 2에는 “Maven 테스트 미실행, 정적 코드 흐름 기준”이라는 불확실성을 해당 항목 안에 포함하세요.
- Finding 4는 실제 호출 경로 또는 최소 재현 예시가 없으면 Medium에서 Low로 낮추고, “현재 실패한 동작”이 아니라 “API 사용 시 함정”으로 재분류하세요.
- Finding 5는 설계 근거가 없으면 Low 유지 또는 “개선 후보”로 옮기고, 필수 버그처럼 보이지 않게 표현하세요.
- Recommendation에는 두 선택지의 조건을 더 명확히 하세요: A Feature 재사용은 동일 게이트 보존, B Feature 유지는 누락 경계값과 상태 검증을 직접 보강해야 함.

## 요약

초안은 핵심 문제를 잘 잡고 있고, 프로젝트 파일과 실행 한계를 대체로 연결하고 있습니다. 다만 일부 claim은 근거 anchor가 부족하거나 구현 결함과 테스트 게이트 결함이 섞여 보입니다. 수정 시 결함의 층위, 정적 판단의 한계, 실제 호출 경로 여부를 분리하면 더 검증 가능한 코드 리뷰가 됩니다.