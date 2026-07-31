# Original User Input

각각의 피드백에 대하여 말한거야 한번에가 아니라


# Checked Context

# 앞선 요청과 이번 명확화

- 앞선 요청은 `task2\assignments\taskB-1.md`의 피드백을 보고 어떤 내용에 어떤 피드백이 담겼는지 설명하고 해결책 2~3개와 trade-off를 보여 달라는 것이었다.
- 이번 요청은 해결책 2~3개가 전체 피드백을 묶은 것이 아니라 각각의 피드백마다 필요하다는 명확화다.

# 확인한 프로젝트 문맥

- `task2/assignments/taskB-1.md`에는 피드백 7건이 있다.
- FB-B1-01과 FB-B1-03은 댓글 없는 이모지 리액션이라 해결할 설계 질문이 없다.
- 실질적인 댓글은 FB-B1-02, FB-B1-04, FB-B1-05, FB-B1-06, FB-B1-07 다섯 건이다.
- FB-B1-02는 잘못 예측한 변경 축과 추상화 비용을 묻는다.
- FB-B1-04는 시스템 바깥에서 들어오는 최초 메시지의 책임을 묻는다.
- FB-B1-05는 서비스 private 메서드에 도메인 책임이 숨어 있을 수 있다는 지적이다.
- FB-B1-06은 외부 정책 API가 상태 전이에 필요할 때 도메인 순수성과 협력을 묻는다.
- FB-B1-07은 새로운 정책이 기존 인터페이스의 입력과 결과로 표현되지 않을 때의 경계 재설계를 묻는다.
- 현재 `RefundProcessor`의 `validateRefundable`은 결제 플랫폼과 주문 상태를 검증한다.
- 현재 `RefundProcessor`의 `calculateRefundAmount`는 enum switch로 PRORATION과 MANUAL 계산을 선택한다.
- 현재 `RefundProcessor`의 `manualAmount`는 수동 환불 금액을 검증한다.
- 현재 `RefundService.cancel`은 PG 호출 뒤 Refund와 Order에 상태 전이 메시지를 보낸다.
- 현재 `Order.applyRefund`는 금액 불변식과 환불 상태 전이를 책임진다.
- 현재 `RefundPolicy`는 인터페이스가 아니라 enum이다. FB-B1-07은 과제 본문에서 제안한 미래 인터페이스에 대한 사고 실험이다.
- `PROBLEM.md`에는 이 요청과 직접 관련된 열린 문제가 없다.

# 성공 기준

- 7개 피드백을 빠짐없이 언급하되 댓글 없는 리액션은 별도로 구분한다.
- 댓글이 있는 5개 피드백 각각에 대해 대상 내용, 질문의 의미, 대안 2~3개, 각 대안의 장단점, 추천을 독립적으로 제시한다.
- 현재 코드의 사실과 리뷰어의 가상 시나리오를 구분한다.
- private 메서드 금지나 인터페이스 분리 같은 설계 휴리스틱을 절대 규칙으로 표현하지 않는다.
