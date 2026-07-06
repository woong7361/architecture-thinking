Feature: 환불 정책 도메인 테스트

  Background:
    Given 현재 시각은 UTC 날짜 기준으로 "2026-07-06"이다

  Scenario Outline: MANUAL 지정은 무료 환불과 일할 계산보다 우선한다
    Given 주문 금액이 <amount>원이고 이미 환불된 금액이 <canceledAmount>원인 <orderStatus> 주문이 있다
    And 결제일은 UTC 날짜 기준 "<paidAt>"이다
    When 관리자가 <manualAmount>원을 수동 환불 금액으로 지정하여 환불 금액을 산출한다
    Then 환불 금액은 <expectedRefundAmount>원이다
    And 환불 유형은 <expectedRefundType>이다

    Examples:
      | amount | canceledAmount | orderStatus        | paidAt     | manualAmount | expectedRefundAmount | expectedRefundType |
      | 30000  | 0              | PAID               | 2026-07-06 | 10000        | 10000                | PARTIAL            |
      | 30000  | 10000          | PARTIALLY_REFUNDED | 2026-06-26 | 20000        | 20000                | FULL               |

  Scenario Outline: 잘못된 MANUAL 지정 금액은 거절된다
    Given 주문 금액이 <amount>원이고 이미 환불된 금액이 <canceledAmount>원인 PAID 주문이 있다
    When 관리자가 <manualAmount>원을 수동 환불 금액으로 지정하여 환불 금액을 산출한다
    Then 환불 금액 산출은 "환불 금액 오류"로 거절된다

    Examples:
      | amount | canceledAmount | manualAmount |
      | 30000  | 0              | 0            |
      | 30000  | 0              | -1           |
      | 30000  | 10000          | 20001        |

  Scenario Outline: MANUAL이 없고 경과일이 7일 이하이면 UTC 날짜 기준으로 전액 환불한다
    Given 주문 금액이 30000원이고 이미 환불된 금액이 0원인 PAID 주문이 있다
    And 결제일은 UTC 날짜 기준 "<paidAt>"이다
    When MANUAL 지정 없이 환불 금액을 산출한다
    Then 환불 금액은 30000원이다
    And 환불 유형은 FULL이다

    Examples:
      | paidAt     |
      | 2026-07-06 |
      | 2026-06-29 |

  Scenario: MANUAL이 없고 경과일이 8일이면 PRORATION 일할 계산을 적용한다
    Given 주문 금액이 30000원이고 이미 환불된 금액이 0원인 PAID 주문이 있다
    And 결제일은 UTC 날짜 기준 "2026-06-28"이다
    And 총 구독 일수는 30일이고 잔여 일수는 15일이다
    When MANUAL 지정 없이 환불 금액을 산출한다
    Then 환불 금액은 15000원이다
    And 환불 유형은 PARTIAL이다

  Scenario Outline: PRORATION은 정수 나눗셈 단가에 잔여 일수를 곱한다
    Given 주문 금액이 <amount>원이고 이미 환불된 금액이 0원인 PAID 주문이 있다
    And 총 구독 일수는 <totalDays>일이고 잔여 일수는 <remainingDays>일이다
    When PRORATION으로 환불 금액을 산출한다
    Then 환불 금액은 <expectedRefundAmount>원이다

    Examples:
      | amount | totalDays | remainingDays | expectedRefundAmount |
      | 30000  | 30        | 30            | 30000                |
      | 30000  | 30        | 15            | 15000                |
      | 30000  | 30        | 1             | 1000                 |
      | 30000  | 30        | 0             | 0                    |
      | 10000  | 30        | 7             | 2331                 |

  Scenario Outline: 잘못된 구독 기간으로 PRORATION을 산출할 수 없다
    Given 주문 금액이 30000원이고 이미 환불된 금액이 0원인 PAID 주문이 있다
    And 총 구독 일수는 <totalDays>일이고 잔여 일수는 1일이다
    When PRORATION으로 환불 금액을 산출한다
    Then 환불 금액 산출은 "구독 기간 오류"로 거절된다

    Examples:
      | totalDays |
      | 0         |
      | -1        |

  Scenario Outline: 환불 가능한 주문 상태와 금액이면 환불을 요청할 수 있다
    Given 주문 금액이 30000원이고 이미 환불된 금액이 <canceledAmount>원인 <orderStatus> 주문이 있다
    When <refundAmount>원 환불 가능 여부를 검증한다
    Then 환불 가능 검증은 성공한다

    Examples:
      | orderStatus        | canceledAmount | refundAmount |
      | PAID               | 0              | 30000        |
      | PAID               | 0              | 15000        |
      | PARTIALLY_REFUNDED | 10000          | 20000        |
      | PARTIALLY_REFUNDED | 10000          | 19999        |

  Scenario Outline: 환불 불가 상태이거나 환불 가능 금액을 초과하면 환불을 요청할 수 없다
    Given 주문 금액이 30000원이고 이미 환불된 금액이 <canceledAmount>원인 <orderStatus> 주문이 있다
    When <refundAmount>원 환불 가능 여부를 검증한다
    Then 환불 가능 검증은 "환불 불가"로 거절된다

    Examples:
      | orderStatus | canceledAmount | refundAmount |
      | REFUNDED    | 30000          | 1            |
      | PENDING     | 0              | 1            |
      | FAILED      | 0              | 1            |
      | PAID        | 0              | 30001        |

  Scenario Outline: 환불 적용 후 주문 상태는 누적 환불 금액으로 결정된다
    Given 주문 금액이 30000원이고 이미 환불된 금액이 <canceledAmount>원인 <orderStatus> 주문이 있다
    When <refundAmount>원 환불을 주문에 적용한다
    Then 주문 상태는 <expectedOrderStatus>이다
    And 이미 환불된 금액은 <expectedCanceledAmount>원이다

    Examples:
      | orderStatus        | canceledAmount | refundAmount | expectedOrderStatus | expectedCanceledAmount |
      | PAID               | 0              | 30000        | REFUNDED            | 30000                  |
      | PAID               | 0              | 29999        | PARTIALLY_REFUNDED  | 29999                  |
      | PARTIALLY_REFUNDED | 10000          | 20000        | REFUNDED            | 30000                  |
      | PARTIALLY_REFUNDED | 10000          | 19999        | PARTIALLY_REFUNDED  | 29999                  |

  Scenario Outline: REQUESTED 환불은 결제 취소 결과에 따라 종료 상태로 전이한다
    Given 환불 요청이 REQUESTED 상태로 생성되어 있다
    When 결제 취소 결과가 "<paymentCancellationResult>"로 확정된다
    Then 환불 상태는 <expectedRefundStatus>이다

    Examples:
      | paymentCancellationResult | expectedRefundStatus |
      | 성공                      | SUCCEEDED            |
      | 명확한 거부               | FAILED               |
      | 결과 불확실               | TIMED_OUT            |
