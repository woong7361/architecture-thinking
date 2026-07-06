Feature: 환불 정책
  결제가 완료된 구독 주문은 정책 우선순위와 환불 가능 금액 안에서만 환불된다.

  Background:
    Given 주문 금액은 30000원이다
    And 이미 환불된 금액은 0원이다
    And 주문 상태는 PAID이다

  Scenario Outline: 정책 우선순위에 따라 환불 금액을 산출한다
    Given 총 구독 일수는 <totalDays>일이다
    And 잔여 일수는 <remainingDays>일이다
    And 결제일로부터 경과일은 <elapsedDays>일이다
    And 수동 지정 금액은 <manualAmount>이다
    When 환불 금액을 산출하면
    Then 환불 금액은 <expectedAmount>원이다
    And 환불 유형은 <refundType>이다

    Examples:
      | totalDays | remainingDays | elapsedDays | manualAmount | expectedAmount | refundType |
      | 30        | 30            | 0           | 10000        | 10000          | PARTIAL    |
      | 30        | 30            | 0           | 없음          | 30000          | FULL       |
      | 30        | 30            | 7           | 없음          | 30000          | FULL       |
      | 30        | 15            | 8           | 없음          | 15000          | PARTIAL    |
      | 30        | 7             | 8           | 없음          | 7000           | PARTIAL    |
      | 30        | 0             | 8           | 없음          | 0              | PARTIAL    |

  Scenario: 일할 계산은 정수 나눗셈으로 소수점 이하를 절사한다
    Given 주문 금액은 10000원이다
    And 이미 환불된 금액은 0원이다
    And 주문 상태는 PAID이다
    And 총 구독 일수는 30일이다
    And 잔여 일수는 7일이다
    And 결제일로부터 경과일은 8일이다
    And 수동 지정 금액은 없음이다
    When 환불 금액을 산출하면
    Then 환불 금액은 2331원이다
    And 환불 유형은 PARTIAL이다

  Scenario Outline: UTC 날짜 기준으로 경과일을 계산해 무료 환불 경계를 판정한다
    Given 결제 시각은 "<paidAt>"이다
    And 환불 요청 시각은 "<requestedAt>"이다
    And 총 구독 일수는 30일이다
    And 잔여 일수는 15일이다
    And 수동 지정 금액은 없음이다
    When 환불 금액을 산출하면
    Then 결제일로부터 경과일은 <elapsedDays>일로 판정된다
    And 환불 금액은 <expectedAmount>원이다

    Examples:
      | paidAt               | requestedAt          | elapsedDays | expectedAmount |
      | 2026-07-01T23:30:00Z | 2026-07-08T00:10:00Z | 7           | 30000          |
      | 2026-07-01T23:30:00Z | 2026-07-09T00:10:00Z | 8           | 15000          |

  Scenario Outline: 잘못된 기간이나 수동 금액은 환불 금액으로 확정할 수 없다
    Given 총 구독 일수는 <totalDays>일이다
    And 잔여 일수는 <remainingDays>일이다
    And 결제일로부터 경과일은 <elapsedDays>일이다
    And 수동 지정 금액은 <manualAmount>이다
    When 환불 금액을 산출하려 하면
    Then 환불 금액 산출은 거절된다

    Examples:
      | totalDays | remainingDays | elapsedDays | manualAmount |
      | 0         | 0             | 8           | 없음          |
      | 30        | 30            | 0           | 0             |
      | 30        | 30            | 0           | -1            |
      | 30        | 30            | 0           | 30001         |

  Scenario Outline: 환불 가능한 주문 상태와 금액만 환불할 수 있다
    Given 주문 상태는 <orderStatus>이다
    And 주문 금액은 30000원이다
    And 이미 환불된 금액은 <canceledAmount>원이다
    When <refundAmount>원을 환불하려 하면
    Then 환불 가능 여부는 <result>이다

    Examples:
      | orderStatus        | canceledAmount | refundAmount | result |
      | PAID               | 0              | 30000        | 허용   |
      | PARTIALLY_REFUNDED | 10000          | 20000        | 허용   |
      | REFUNDED           | 30000          | 1            | 거절   |
      | PENDING            | 0              | 1            | 거절   |
      | FAILED             | 0              | 1            | 거절   |
      | PARTIALLY_REFUNDED | 10000          | 20001        | 거절   |

  Scenario Outline: 성공한 환불 금액에 따라 주문 상태가 전이된다
    Given 주문 상태는 <beforeStatus>이다
    And 주문 금액은 30000원이다
    And 이미 환불된 금액은 <canceledAmount>원이다
    When <refundAmount>원의 환불이 성공하면
    Then 주문 상태는 <afterStatus>이다
    And 이미 환불된 금액은 <expectedCanceledAmount>원이다

    Examples:
      | beforeStatus       | canceledAmount | refundAmount | afterStatus        | expectedCanceledAmount |
      | PAID               | 0              | 30000        | REFUNDED           | 30000                  |
      | PAID               | 0              | 15000        | PARTIALLY_REFUNDED | 15000                  |
      | PARTIALLY_REFUNDED | 10000          | 20000        | REFUNDED           | 30000                  |
      | PARTIALLY_REFUNDED | 10000          | 15000        | PARTIALLY_REFUNDED | 25000                  |

  Scenario Outline: 결제 취소 결과에 따라 환불 상태가 전이된다
    Given 환불 요청이 생성되어 상태는 REQUESTED이다
    When 결제 취소 결과가 <cancellationResult>이면
    Then 환불 상태는 <refundStatus>이다

    Examples:
      | cancellationResult | refundStatus |
      | 성공               | SUCCEEDED    |
      | 명확한 거부        | FAILED       |
      | 결과 불확실        | TIMED_OUT    |
