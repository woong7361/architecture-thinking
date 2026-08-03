# Original User Input

다음 개선에서는 L1 게이트에 PgChargeAdapter 전용 HTTP 계약 테스트를 추가해야 한다. 로컬 mock PG를 실제 HTTP로 호출해 요청이 POST /charge 로 전송되는지, 본문에 paymentInfo 와 amount 가 들어가는지, HTTP 200의 approved:true 와 approved:false 를 각각 승인과 거절로 해석하는지를 자동 판정한다. 그러면 /payments 추측은 요청 매핑 불일치로 실패하고, 2xx를 곧 승인으로 보는 구현은 거절 시나리오에서 실패한다. 더 크게 바꾸려면 저장과 결제를 서로 다른 레이어로 분리해 각 슬롯을 별도 게이트로 승격할 수도 있다. 하지만 현재 구조에서는 전용 계약 테스트를 L1 게이트에 추가하는 편이 변경이 작고 실패 원인도 명확하다.

task3\assignments\taskC-6.md 에서 비어있는 테스트 커버리지를 발견해서 그걸 입력을 바꾸어서 넣다는거지?

그리고 다음 개선에는 input을 더 조인다는거고 

근데 여기서는 어떻게 해야할지가 고민이네? 내가 못잡은 
계약을 AI가 임의로 추가하는건 너무 위험할수도 있고, 그렇다고 그냥 넘기자니 잠재 위험을 남겨두는거잖아? 

그러면 어떻게 하냐면 내 생각은 여기서는 test green을 단일 게이트로 삼되 문제가 있는 case가 나온다면 report를 보내는게 좋지 않
을까?


내 생각이 맞아?


# Checked Context

# Project Context

- `task3/assignments/taskC-6.md` records two distinct failures: omission of the payment contract from the first L1 input caused an incorrect implementation, while test coverage that replaced `ChargePort` with a double allowed that implementation to receive GREEN.
- The current `.codex/skills/skeleton-agent/pipeline/inputs/c6-ticket-skeleton.json` already contains the L1 HTTP contract at lines 89-93: POST `/charge`, request fields `paymentInfo` and `amount`, response field `approved`, both decisions using HTTP 200, and body-based approval.
- The same input's `storage` configuration executes only `JpaCucumberAcceptanceTest`; its `protocol` description explicitly says payment remains a double. Therefore the current gate does not execute `PgChargeAdapter`.
- `task3/ticket-reservation-c6/docker/mock-pg/mappings/charge-approved.json` and `charge-declined.json` define the local fixture behavior. They establish the assignment's local mock behavior, but do not by themselves prove an actual external PG provider contract unless the human owner designates or validates them as the source of truth.
- `.codex/skills/skeleton-agent/runs/c6/L1/review.md` records that the first incorrect adapter passed all automated tests and was rejected after a human followed an AI note and inspected the mapping.
- Decision to evaluate: retain test GREEN as the only automatic correctness gate without allowing AI-discovered assumptions to become authoritative tests, while preserving material uncertainty through reporting and human acceptance.
- Project rules require multiple options and trade-offs, prohibit unsupported inference, and favor verifiable outcomes.
