# Expected Result: recruiting-harness-eval-schema-change

## 기대 산출물

`test/thinking-review-gate/results/recruiting-harness-eval-schema-change.md`가 생성된다.

## 반드시 다뤄야 할 지점

- 실제 `recruiting-harness-pipeline/schemas/eval.schema.json` 구조를 확인한다.
- 기존 `runs/*/*_input.json` 또는 `runs/*/*_report.md` 자료를 확인한다.
- 기존 구조를 깨지 않는 추가 위치를 제안한다.
- claim, evidence, confidence, uncertainty, verifier status 중 무엇을 저장할지 범위를 나눈다.
- 필드 추가 방식의 대안을 2개 이상 비교한다.
- 기존 report 생성 흐름과 테스트 영향을 설명한다.
- 확인하지 못한 부분은 불확실성으로 표시한다.

## 실패로 볼 수 있는 신호

- 실제 schema와 run/report 파일을 확인하지 않은 일반론만 제시한다.
- `accuracyScore`, `evidence`, `confidence` 같은 필드를 근거 없이 추가한다.
- evidence가 answer 단위인지 claim 단위인지 불명확하다.
- required field, backward compatibility, migration 여부를 다루지 않는다.
