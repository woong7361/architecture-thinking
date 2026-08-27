# 현재 글의 핵심 문장

- "나머지 16개는 통과만 봤다."
- "이 16개에는 아직 게이트라고 믿을 근거가 부족했다."
- "정책을 승인하는 자리는 만들었지만, 연결 코드가 실제로 게이트를 닫는지 확인하는 자리는 아직 비어 있다."

# 실제 연결 코드 A

`RefundStepDefinitions`의 When은 `refundProcessor.refund(order, request)`를 호출한다. Then은 반환된 `RefundReceipt`의 금액과 유형, 주문 상태, 환불 가능 금액, 예외 사유를 AssertJ로 비교한다. 복잡한 환불 정책 계산은 연결 코드에 다시 쓰지 않았다.

# 실제 연결 코드 B

`RefundFeatureStepDefinitions`의 Then은 반환 금액, 유형, 주문 상태, 오류를 비교한다. 다만 When에서는 하나의 공개 유스케이스만 호출하지 않는다. `order.validateRefundable()`, `RefundPolicy.calculate(...)`, `Refund.requested(...)`, `order.applyRefund(...)`를 순서대로 호출하며 오케스트레이션 일부가 연결 코드 안에 있다.

# 이미 확인된 결과

18개 시나리오 가운데 두 개는 실제 구현에서 빠진 정책 때문에 실패했다. 나머지 16개는 통과했다. 아직 의도적으로 결함을 주입해 16개 각각이 실패하는지는 확인하지 않았다.
