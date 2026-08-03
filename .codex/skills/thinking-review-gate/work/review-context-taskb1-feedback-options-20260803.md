# 확인한 문맥

- 직전 대화에서 단순 이모지와 칭찬·동의 표현을 제외한 실질적인 피드백 5건을 추렸다.
- FB-B1-02는 잘못 예측한 변경 축과 추상화 비용을 묻는다.
- FB-B1-04는 시스템 외부에서 들어오는 최초 메시지의 책임을 묻는다.
- FB-B1-05는 서비스 private 메서드에 도메인 책임이 남아 있을 가능성을 지적한다.
- FB-B1-06은 외부 정책 API가 상태 전이에 필요할 때 도메인 순수성과 협력을 묻는다.
- FB-B1-07은 새 정책이 기존 인터페이스의 입력과 결과로 표현되지 않을 때 경계 재설계를 묻는다.
- `task1/task1-4-history-A/.../RefundProcessor.java`는 주문 검증, 정책 switch, 수동 금액 검증을 private 메서드로 가진다.
- 위 코드의 정책 분기는 현재 `PRORATION`, `MANUAL` 두 종류이며 한 메서드에만 있다.
- `task1/task1-4-history-A/.../RefundPolicy.java`는 인터페이스가 아니라 enum이다.
- `task1/task1-3-history/.../RefundService.java`는 PG 호출을 조율하고 결과에 따라 `Refund`와 `Order`에 상태 전이 메시지를 보낸다.
- `task1/task1-3-history/.../PgClient.java`는 외부 PG를 감싼 포트다.
- `task1/task1-4-history-B/.../Order.java`는 환불 가능 상태와 금액 불변식, 상태 전이를 책임진다.
- `task1/task1-4-history-B/.../RefundCalculationRequest.java`는 여러 정책의 입력을 한 record에 모으고 `manualAmount`를 nullable로 가진다. 이는 범용 context가 커질 때의 위험을 보여주는 현재 코드 근거다.
- `task2/CLAUDE.md`는 확인된 변경 축에만 OCP 추상화를 적용하고, private 메서드 분리를 책임 분리로 보지 않으며, 협력에서 설계를 시작하도록 규정한다.
- `PROBLEM.md`에는 이 요청과 직접 관련된 열린 문제가 검색되지 않았다.

# 성공 기준

- 실질적인 피드백 5건을 모두 다룬다.
- 각 피드백마다 2~3개 해결 방안 또는 의견을 제시한다.
- 각 방안마다 적용 예시와 장점, 비용 또는 실패 가능성을 설명한다.
- 현재 코드에 적합한 추천과 요구가 커졌을 때의 전환 조건을 명시한다.
- 현재 구현 사실과 리뷰어가 제시한 가상 시나리오를 구분한다.
- 모든 private 메서드 금지, 모든 switch 제거, 모든 외부 의존의 도메인 포트화처럼 휴리스틱을 절대 규칙으로 만들지 않는다.
