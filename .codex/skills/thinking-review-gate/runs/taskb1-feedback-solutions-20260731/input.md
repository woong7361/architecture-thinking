# Original User Input

task2\assignments\taskB-1.md 의 피드백을 보고 
어떤 내용에 대해 어떤 피드백이 담겼는지 그 해결책은 무엇인지 2-3지를 제안하고 트레이드오프를 보여줘


# Checked Context

# 확인한 프로젝트 문맥

- `task2/assignments/taskB-1.md` 본문과 Notion 피드백 7건을 UTF-8로 확인했다.
- FB-B1-01과 FB-B1-03은 댓글 없이 이모지 리액션만 있다.
- FB-B1-02는 변경을 국소화한다는 주장에 대해, 예상한 변경 축이 틀리면 추상화 자체가 장애물이 될 수 있음을 묻는다.
- FB-B1-04는 역할·책임·협력에서 시스템 밖의 최초 메시지를 누가 받을지 묻는다.
- FB-B1-05는 서비스의 private 메서드에 도메인 행위가 숨어 있을 가능성을 지적한다.
- FB-B1-06은 상태 전이에 외부 환불 정책 API가 필수일 때 도메인 순수성과 외부 협력을 함께 지킬 방법을 묻는다.
- FB-B1-07은 새 환불 정책이 기존 계약의 입력·출력으로 표현되지 않을 때 다형성 경계를 어떻게 다룰지 묻는다.
- 현재 `RefundService.cancel`은 PG 호출과 도메인 상태 전이를 조율한다.
- 현재 `RefundProcessor`에는 `validateRefundable`, `calculateRefundAmount`, `manualAmount`라는 private 메서드가 있으며 정책 선택은 enum switch다.
- 현재 `Order.applyRefund`는 금액 검증과 상태 전이를 객체 안에 둔다.
- 현재 코드의 `RefundPolicy`는 인터페이스가 아니라 `PRORATION`, `MANUAL` 값을 가진 enum이다. 과제 본문은 이를 인터페이스로 바꾸는 가상 대안을 설명한다.
- `PROBLEM.md`에는 이 주제와 직접 연결된 열린 문제가 없다.

# 답변 제약

- 피드백이 겨냥한 원문과 피드백의 의미를 구분한다.
- 해결 방향 2~3개와 각각의 장점, 비용, 적용 조건을 제시한다.
- 확인된 사실과 설계 해석을 구분하고, private 메서드나 인터페이스에 관한 규칙을 절대화하지 않는다.
- 현재 프로젝트에서 우선할 추천을 명시한다.
