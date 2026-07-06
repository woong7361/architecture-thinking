Feature: 환불 정책
  결제가 완료된 구독 주문은 정책 우선순위와 환불 가능 금액에 따라 환불 금액과 상태가 결정되어야 한다.

  Scenario: 수동 지정 금액은 7일 이하 무료와 일할 계산보다 우선한다
    Given 주문 상태는 PAID이다
    And 주문금액은 30000원이고 이미 환불된 금액은 0원이다
    And 총 구독 일수는 30일이고 잔여 일수는 27일이다
    And 결제일로부터 3일이 지났다
    When 관리자가 환불 금액을 5000원으로 지정하여 환불 금액을 산출한다
    Then 환불 금액은 5000원이다
    And 환불 유형은 PARTIAL이다

  Scenario: 부분 환불 후 수동 지정 금액이 환불 가능 금액과 같으면 FULL이다
    Given 주문 상태는 PARTIALLY_REFUNDED이다
    And 주문금액은 30000원이고 이미 환불된 금액은 10000원이다
    And 총 구독 일수는 30일이고 잔여 일수는 27일이다
    And 결제일로부터 3일이 지났다
    When 관리자가 환불 금액을 20000원으로 지정하여 환불 금액을 산출한다
    Then 환불 금액은 20000원이다
    And 환불 유형은 FULL이다

  Scenario Outline: 수동 지정 금액이 허용 범위를 벗어나면 환불 금액을 확정하지 않는다
    Given 주문 상태는 <orderStatus>이다
    And 주문금액은 30000원이고 이미 환불된 금액은 <alreadyRefundedAmount>원이다
    When 관리자가 환불 금액을 <manualAmount>원으로 지정하여 환불 금액을 산출한다
    Then 환불 금액 산출은 거절된다
    And 환불 금액은 확정되지 않는다

    Examples:
      | orderStatus        | alreadyRefundedAmount | manualAmount |
      | PAID               | 0                     | 0            |
      | PAID               | 0                     | -1           |
      | PARTIALLY_REFUNDED | 10000                 | 20001        |

  Scenario Outline: 결제일로부터 7일 이하이면 수동 지정이 없을 때 환불 가능 금액을 전액 환불한다
    Given 주문 상태는 <orderStatus>이다
    And 주문금액은 30000원이고 이미 환불된 금액은 <alreadyRefundedAmount>원이다
    And 총 구독 일수는 30일이고 잔여 일수는 <remainingDays>일이다
    And 결제일로부터 <elapsedDays>일이 지났다
    When 수동 지정 없이 환불 금액을 산출한다
    Then 환불 금액은 <refundAmount>원이다
    And 환불 유형은 FULL이다

    Examples:
      | orderStatus        | alreadyRefundedAmount | elapsedDays | remainingDays | refundAmount |
      | PAID               | 0                     | 0           | 30            | 30000        |
      | PAID               | 0                     | 7           | 23            | 30000        |
      | PARTIALLY_REFUNDED | 10000                 | 7           | 23            | 20000        |

  Scenario Outline: UTC 날짜 기준으로 경과일 경계를 판정한다
    Given 주문 상태는 PAID이다
    And 주문금액은 30000원이고 이미 환불된 금액은 0원이다
    And 총 구독 일수는 30일이고 잔여 일수는 <remainingDays>일이다
    And 결제 시각은 <paidAt> UTC이다
    And 환불 요청 시각은 <requestedAt> UTC이다
    When 수동 지정 없이 환불 금액을 산출한다
    Then 경과일은 <elapsedDays>일이다
    And 적용된 환불 규칙은 <appliedRule>이다
    And 환불 금액은 <refundAmount>원이다
    And 환불 유형은 <refundType>이다

    Examples:
      | paidAt               | requestedAt          | elapsedDays | remainingDays | appliedRule | refundAmount | refundType |
      | 2026-07-01T00:30:00Z | 2026-07-01T23:30:00Z | 0           | 30            | 7일 이하 무료  | 30000        | FULL       |
      | 2026-07-01T23:30:00Z | 2026-07-08T00:30:00Z | 7           | 23            | 7일 이하 무료  | 30000        | FULL       |
      | 2026-07-01T23:30:00Z | 2026-07-09T00:00:00Z | 8           | 22            | PRORATION   | 22000        | PARTIAL    |

  Scenario: 결제일로부터 8일이 지나면 일할 계산을 적용한다
    Given 주문 상태는 PAID이다
    And 주문금액은 30000원이고 이미 환불된 금액은 0원이다
    And 총 구독 일수는 30일이고 잔여 일수는 22일이다
    And 결제일로부터 8일이 지났다
    When 수동 지정 없이 환불 금액을 산출한다
    Then 환불 금액은 22000원이다
    And 환불 유형은 PARTIAL이다

  Scenario: 만료 후 일할 계산 결과는 0원이다
    Given 주문 상태는 PAID이다
    And 주문금액은 30000원이고 이미 환불된 금액은 0원이다
    And 총 구독 일수는 30일이고 잔여 일수는 0일이다
    And 결제일로부터 30일이 지났다
    When 수동 지정 없이 환불 금액을 산출한다
    Then 환불 금액은 0원이다

  Scenario Outline: 일할 계산은 잔여 일수와 정수 단가로 금액을 산출한다
    Given 일할 계산 대상 결제금액은 <paidAmount>원이고 총 구독 일수는 <totalDays>일이다
    When 잔여 일수가 <remainingDays>일인 일할 환불 금액을 산출한다
    Then 일할 환불 금액은 <refundAmount>원이다

    Examples:
      | paidAmount | totalDays | remainingDays | refundAmount |
      | 30000      | 30        | 30            | 30000        |
      | 30000      | 30        | 1             | 1000         |
      | 30000      | 30        | 0             | 0            |
      | 10000      | 30        | 7             | 2331         |

  Scenario Outline: 총 구독 일수가 0일 이하이면 일할 계산을 거절한다
    Given 일할 계산 대상 결제금액은 30000원이고 총 구독 일수는 <totalDays>일이다
    When 잔여 일수가 1일인 일할 환불 금액을 산출한다
    Then 일할 환불 금액 산출은 거절된다
    And 일할 환불 금액은 확정되지 않는다

    Examples:
      | totalDays |
      | 0         |
      | -1        |

  Scenario Outline: 환불 가능한 주문 상태에서 환불 금액이 환불 가능 금액 이하이면 환불 요청이 가능하다
    Given 주문 상태는 <orderStatus>이다
    And 주문금액은 30000원이고 이미 환불된 금액은 <alreadyRefundedAmount>원이다
    When <refundAmount>원의 환불 가능 여부를 확인한다
    Then 환불 가능 여부 확인은 허용된다
    And 주문 상태는 <orderStatus>이다
    And 환불 가능 금액은 <cancellableAmount>원이다

    Examples:
      | orderStatus        | alreadyRefundedAmount | cancellableAmount | refundAmount |
      | PAID               | 0                     | 30000             | 15000        |
      | PAID               | 0                     | 30000             | 30000        |
      | PARTIALLY_REFUNDED | 15000                 | 15000             | 10000        |
      | PARTIALLY_REFUNDED | 15000                 | 15000             | 15000        |

  Scenario Outline: 환불할 수 없는 주문 상태이거나 환불 가능 금액을 초과하면 환불 요청이 거절된다
    Given 주문 상태는 <orderStatus>이다
    And 주문금액은 30000원이고 이미 환불된 금액은 <alreadyRefundedAmount>원이다
    When <refundAmount>원의 환불 가능 여부를 확인한다
    Then 환불 가능 여부 확인은 거절된다
    And 주문 상태는 <orderStatus>이다
    And 환불 가능 금액은 <cancellableAmount>원이다
    And 환불 요청은 생성되지 않는다

    Examples:
      | orderStatus        | alreadyRefundedAmount | cancellableAmount | refundAmount |
      | REFUNDED           | 30000                 | 0                 | 1            |
      | PENDING            | 0                     | 30000             | 1000         |
      | FAILED             | 0                     | 30000             | 1000         |
      | PAID               | 0                     | 30000             | 30001        |
      | PARTIALLY_REFUNDED | 15000                 | 15000             | 15001        |

  Scenario Outline: 환불 성공 후 주문은 누적 환불 금액에 따라 상태가 전이된다
    Given 주문 상태는 <beforeStatus>이다
    And 주문금액은 30000원이고 이미 환불된 금액은 <alreadyRefundedAmount>원이다
    When <refundAmount>원의 환불 성공을 주문에 적용한다
    Then 주문 상태는 <afterStatus>이다
    And 환불 가능 금액은 <remainingCancellableAmount>원이다

    Examples:
      | beforeStatus       | alreadyRefundedAmount | refundAmount | afterStatus        | remainingCancellableAmount |
      | PAID               | 0                     | 30000        | REFUNDED           | 0                          |
      | PAID               | 0                     | 15000        | PARTIALLY_REFUNDED | 15000                      |
      | PARTIALLY_REFUNDED | 15000                 | 15000        | REFUNDED           | 0                          |
      | PARTIALLY_REFUNDED | 10000                 | 15000        | PARTIALLY_REFUNDED | 5000                       |

  Scenario Outline: 환불 요청은 결제 취소 결과에 따라 환불 상태가 결정된다
    Given 15000원의 환불 요청이 REQUESTED 상태로 생성되어 있다
    When 결제 취소 결과가 <paymentCancelResult>이다
    Then 환불 상태는 <refundStatus>이다

    Examples:
      | paymentCancelResult | refundStatus |
      | 성공                | SUCCEEDED    |
      | 명확한 거부         | FAILED       |
      | 응답 불확실         | TIMED_OUT    |