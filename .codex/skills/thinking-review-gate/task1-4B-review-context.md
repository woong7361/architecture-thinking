# Checked Context

- Requested directory `task1-5-history-B` does not exist in the workspace. The existing matching directory and open tabs indicate `task1/task1-4-history-B`, so review assumes that target.
- Design source: `task1/refund_design.md`.
- Target code source: `task1/task1-4-history-B/src/main/java/com/thinking/payment/domain`.
- Target contains only domain classes/enums: `Order`, `Refund`, `RefundPolicy`, `RefundCalculationRequest`, statuses/types, exception.
- No `src/test` directory exists under `task1/task1-4-history-B`.
- `mvnw.cmd test` failed before build because `JAVA_HOME` is not defined correctly. Local `java`/`javac` are version 16 while `pom.xml` requests compiler release 17.
- Source-only compile was checked separately with `javac -encoding UTF-8 -d $TEMP/...` and passed.

Relevant design anchors:

- `refund_design.md:21-24`: scope includes refund amount calculation, order state transition, refund entity state transition; external payment/DB/subscription cancellation are out of scope.
- `refund_design.md:82`: unit-test range is order refundable validation, refund amount calculation, refund/order state transition.
- `refund_design.md:101`: refundable initial states are `PAID`, `PARTIALLY_REFUNDED`; `REFUNDED`, `PENDING`, `FAILED` are not refundable.
- `refund_design.md:148`: MANUAL is highest priority and independent of 7-day free/proration.
- `refund_design.md:158-166`: elapsed days <= 7 gives full refund, after MANUAL and before PRORATION.
- `refund_design.md:169-177`: priority is MANUAL -> <=7 days free -> PRORATION.
- `refund_design.md:186-187`: refund type is FULL if refund amount equals cancellable amount, otherwise PARTIAL.
- `refund_design.md:241-243`: manual amount <= 0 disallowed, cancellable amount is amount - canceledAmount, priority is MANUAL > 7-day free > PRORATION.
- `refund_design.md:244`: concurrency decision says pessimistic lock for order and optimistic lock for REQUESTED state. This may be out of scope for pure in-memory domain code but is a noted design decision.

Relevant implementation anchors:

- `Order.java:28-39`: constructor validates amount and canceledAmount range, but does not validate consistency between status and canceledAmount.
- `Order.java:41-51`: refundable states are PAID and PARTIALLY_REFUNDED.
- `Order.java:58-64`: cancellable amount and refund type use current canceledAmount.
- `Order.java:67-77`: apply refund validates refundable and amount, but returns on zero refund without state change.
- `Order.java:83-90`: refund amount validation allows zero and rejects negative/excess.
- `Refund.java:24-31`: constructor permits arbitrary type/status pairing and validates only non-negative amount.
- `Refund.java:70-75`: terminal transitions only from REQUESTED.
- `RefundPolicy.java:11-15`: enum instance method `calculate(amount,totalDays,remainingDays)` performs proration directly and bypasses 7-day rule.
- `RefundPolicy.java:25-44`: request-based calculation applies manual first, then elapsedDays <= 7, then proration and cancellable validation.
- `RefundPolicy.java:111-119`: manual amount requires non-null positive amount and checks cancellable amount.
- `RefundCalculationRequest.java:16-31`: request constructor always validates amount, cancellableAmount, totalDays, remainingDays, elapsedDays before policy priority is applied.
- `RefundCalculationRequest.java:71-79`: elapsed days are calculated by UTC date and reject requestedAt before paidAt.
