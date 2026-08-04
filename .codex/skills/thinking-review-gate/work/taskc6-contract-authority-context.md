# Project Context

- `task3/assignments/taskC-6.md` records two distinct failures: omission of the payment contract from the first L1 input caused an incorrect implementation, while test coverage that replaced `ChargePort` with a double allowed that implementation to receive GREEN.
- The current `.codex/skills/skeleton-agent/pipeline/inputs/c6-ticket-skeleton.json` already contains the L1 HTTP contract at lines 89-93: POST `/charge`, request fields `paymentInfo` and `amount`, response field `approved`, both decisions using HTTP 200, and body-based approval.
- The same input's `storage` configuration executes only `JpaCucumberAcceptanceTest`; its `protocol` description explicitly says payment remains a double. Therefore the current gate does not execute `PgChargeAdapter`.
- `task3/ticket-reservation-c6/docker/mock-pg/mappings/charge-approved.json` and `charge-declined.json` define the local fixture behavior. They establish the assignment's local mock behavior, but do not by themselves prove an actual external PG provider contract unless the human owner designates or validates them as the source of truth.
- `.codex/skills/skeleton-agent/runs/c6/L1/review.md` records that the first incorrect adapter passed all automated tests and was rejected after a human followed an AI note and inspected the mapping.
- Decision to evaluate: retain test GREEN as the only automatic correctness gate without allowing AI-discovered assumptions to become authoritative tests, while preserving material uncertainty through reporting and human acceptance.
- Project rules require multiple options and trade-offs, prohibit unsupported inference, and favor verifiable outcomes.
