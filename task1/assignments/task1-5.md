(Grit's Why): 이것이 이번 과제의 진짜 산출물입니다. Phase 0의 글쓰기 하네스 구조(Gen, Critique, Eval, Refine, Validate와 JSON 스키마, YAML 루브릭)를 코드 테스트 도메인으로 이식하세요. 처음부터 다시 만들지 마세요. (하네스가 아직 없다면 이번 Task에서 v0를 처음 세우는 것까지가 범위입니다.) 현웅님이 Phase 0 회고에서 '백엔드 코드도 기능·보안·성능·유지보수성·감사 가능성으로 점수를 매겨보면 어떨까' 하셨는데, 그 발상이 바로 이 파이프라인의 루브릭입니다.

### 파이프라인 4단 (Gen과 Eval 분리 유지)

- Step 1 Gen: 자연어 요구사항을 입력하면 테스트 케이스(단위 테스트와 Gherkin 시나리오) 초안을 뽑는 에이전트.
- Step 2 Critique: 새 세션에 '시니어 QA' 역할을 주고 빈틈(빠진 경계값, 누락된 실패 케이스, 모호한 Then, Mock 남용) 세 가지를 지적하게 합니다.
- Step 3 Eval: 루브릭의 다축 가중 구조를 테스트용으로 교체합니다. 축은 coverage(경계와 실패 포함), unambiguity(검증 가능한 단언인가), independence(시나리오 독립성), executability(Step으로 구현 가능한가)를 예시로 삼으세요.
- Step 4 Validate: 본인 계약 게이트를 재사용합니다. 시나리오 JSON 스키마를 체크하고, 'should work' 같은 검증 불가능한 단언을 금지 패턴으로 걸러내며, 루브릭 min_total 미달 시 REJECT합니다.

### 수행 내용

1. Phase 0 하네스 구조를 재사용해 테스트 생성 파이프라인으로 확장해 주세요. 어디를 재사용했고 무엇을 새로 만들었는지 적어 주세요. (하네스가 없었다면 v0를 세운 과정을 적어 주세요.)
2. 실제로 한 번 돌려서 A-3와 A-4의 요구사항을 파이프라인에 통과시켜 주세요. 파이프라인이 생성한 테스트와 본인이 손으로 쓴 테스트를 나란히 비교해 주세요.
3. 실행 로그를 보존하고, MAX_ITERATIONS 몇 번에서 수렴했는지 기록해 주세요.

### 제출물

- [ ]  파이프라인 코드와 프롬프트, 스키마, 루브릭을 GitHub에.
- [ ]  파이프라인이 생성한 테스트와 본인이 손으로 쓴 테스트를 비교한 정리.
- [ ]  실행 로그와 수렴 기록.

---

## 제출물 위치 (워크스페이스 루트 기준 상대 경로)

루트는 `architecture-thinking/`.

### 파이프라인 코드 · 프롬프트 · 스키마 · 루브릭

- 파이프라인 골격: [.codex/skills/generate-test/pipeline/runner.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/generate-test/pipeline/runner.py) — `--mode {contract,unit,bundled}`가 gen 프롬프트·rubric·runs 그룹을 함께 고른다. 그 외 `intake_to_input.py`, `validate.py`, `run_draft.py`, `stages/` 동일 디렉토리.
- 오케스트레이션(동결 게이트 포함): [.codex/skills/generate-test/SKILL.md](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/generate-test/SKILL.md)
- 프롬프트: [.codex/skills/generate-test/pipeline/prompts/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/generate-test/pipeline/prompts/)
  - Gen(모드별): `gen_contract.md` · `gen_unit.md` · `gen_bundled.md`
  - 공유 3단: `critique_system.md` · `eval_system.md` · `refine_system.md`
- 스키마: [.codex/skills/generate-test/pipeline/schemas/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/generate-test/pipeline/schemas/)
  - 입력/산출: `input.schema.json` · `gen_output.schema.json` · `final.schema.json`
  - eval(축-불가지론 + 모드별 closed): `eval.schema.json` · `eval_output.{contract,unit,bundled}.schema.json`
- 루브릭: [.codex/skills/generate-test/pipeline/rubrics/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/generate-test/pipeline/rubrics/) — `contract.rubric.yaml` · `unit.rubric.yaml` · `bundled.rubric.yaml`

### 실행 로그 · 수렴 기록

split run 그룹 루트: [.codex/skills/generate-test/runs/split/2026-07-06_f1ba346d/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/generate-test/runs/split/2026-07-06_f1ba346d/)

| 스트림 | 경로 | iteration별 로그 | 수렴 |
|---|---|---|---|
| contract | `contract/2026-07-06_f1ba346d/` | `iter_001/`·`iter_002/`의 `*_draft.json`·`*_critique.json`·`*_eval.json`(+`eval.validation.json`) | `f1ba346d_final.json` → `final_iteration: "002"` |
| unit | `unit/2026-07-06_7f1fd837/` | `iter_001/`~`iter_003/` 동일 구성 | `7f1fd837_final.json` → `final_iteration: "003"` |

- 각 `*_eval.json`에 축 점수 · `axis_rationales` · `rubric_name` · `weighted_total`이 남는다(비교 정리의 근거).
- 수렴 요약(`weighted_total`·축 점수)은 각 `*_final.json`의 `quality_snapshot`에 있다: contract 4.3/5, unit 4.675/5.
- 생성 최종 아티팩트: `.../contract/.../artifact/refund.feature`, `.../unit/.../artifact/{RefundCalculatorTest,RefundApplicationServiceTest,OrderTest,RefundTest}.java`.

---

## 파이프라인 생성 테스트 vs 손으로 쓴 테스트 — 비교 정리

비교한 대상은 두 쪽이다. 손으로 쓴 쪽은 task1-3에서 직접 짠 단위테스트와 task1-4-A에서 직접 쓴 Cucumber 인수테스트다. 파이프라인 쪽은 split run `runs/split/2026-07-06_f1ba346d/`가 뽑아낸 gherkin 계약과 JUnit 단위테스트다. 같은 환불 도메인을 두고 손과 파이프라인이 각각 무엇을 잡고 무엇을 놓쳤는지 대봤다.

### 파이프라인이 나았던 점

가장 인상적이었던 건, 정책만 적어 넣은 얇은 입력에서 파이프라인이 제법 두꺼운 도메인 모델을 스스로 세웠다는 점이다. 나는 task1-3에서 환불 계산을 `calculate(price, totalDays, elapsedDays)`처럼 int 세 개로 좁게 봉인했는데, 파이프라인은 주문 애그리게이트, 환불 가능 금액 한도, 환불 유형 판정, 누적 환불, PG 취소 결과 전이까지 task1-4 수준의 모델을 알아서 끌어냈다. 커버리지 밀도만 보면 내가 손으로 짠 것보다 촘촘하다.

내가 아예 생각하지 못한 경계도 만들어냈다. 대표적인 게 UTC 시분초를 잘라내고 날짜만으로 7일 무료 경계를 판정하는 케이스다. 나는 경과일을 그냥 정수로 넘겨서 이 경계 자체가 존재하지 않았는데, 파이프라인은 시간 모델을 도입하면서 새 경계를 파생시켰다.

그리고 무엇보다 빠르고 편하다. 요구사항에서 테스트 초안까지 몇 분이면 나온다. 손으로 삼각측량하며 Red-Green-Refactor 사이클을 하나씩 도는 것과는 비교가 안 된다.

### 손으로 쓴 쪽이 나았던 점

먼저, 작업하는 도중에 떠오른 규칙을 파이프라인은 담아내지 못했다. 내가 손으로 쓴 feature에는 애플 앱스토어나 구글 플레이로 결제된 주문은 이 경로로 환불할 수 없다는 시나리오가 네 개 들어 있다. 이건 처음 요구사항에 적혀 있던 게 아니라, 시나리오를 하나씩 짜 내려가다가 "결제 플랫폼이 다르면 환불 경로도 달라야 하지 않나" 하고 중간에 떠올린 규칙이다. 손으로 할 때는 이렇게 도중에 생각난 걸 그 자리에서 바로 시나리오로 밀어 넣을 수 있었다. 반면 파이프라인은 입력을 미리 고정해서 한 번에 돌리기 때문에, 돌리는 동안 내가 새로 떠올린 축을 끼워 넣을 자리가 없다. AI가 알아서 끝까지 뽑아 주는 대신, 작업하면서 생각을 보태는 여지가 그만큼 좁아지는 셈이다.

두 번째로, 생성된 gherkin에는 기술 용어가 새어 나왔다. Feature는 도메인 언어로만 써야 하고 클래스명이나 enum 같은 구현 세부는 드러내지 않는 게 원칙인데, 파이프라인은 코드의 enum 값을 문장에 그대로 박아 넣었다. 내가 손으로 쓴 feature는 같은 내용을 도메인 말로 옮겨 적었다.

손으로 쓴 feature:
```gherkin
그러면 환불 유형은 전액 환불이다
그리고 주문 상태는 부분 환불됨이다
```
파이프라인이 생성한 계약:
```gherkin
And 환불 유형은 FULL이다
And 주문 상태는 REFUNDED가 된다
And 환불 상태는 REQUESTED를 거쳐 SUCCEEDED가 된다
And 결제 UTC 날짜는 2026-07-01이고 ...
```

`FULL`, `REFUNDED`, `REQUESTED`, `SUCCEEDED`, `UTC` 같은 표현은 도메인 전문가의 말이 아니라 코드에 정의된 상태값이자 기술 용어다. eval도 이걸 잡아내서 behavioral_altitude 축을 만점이 아니라 4.0으로 깎았고, 근거에는 "UTC 시각·REQUESTED 같은 기술·상태 표현은 정책 용어라 허용 가능한 수준"이라고 남겼다. 사람이 한 번 번역해 준 손 feature가 이 지점에서는 더 깨끗하다.

한 가지는 우열이 아니라 위치의 차이로 봤다. 내가 손으로 쓴 테스트는 "왜 이 경계인가"를 코드 주석에 바로 적어 둔다. 파이프라인은 그 근거를 사라뜨리지 않고 `runs/`의 eval 로그에 구조화해서 남긴다. 사람이 읽는 자리에서 기계가 읽는 자리로 옮겨갔을 뿐이라, 어느 쪽이 낫다고 하긴 어렵다.

### 정리하며

결국 장단점이 갈렸다. 파이프라인은 얇은 입력에서 두꺼운 모델과 촘촘한 경계를 순식간에 뽑아내지만, 돌리는 도중에 새로 떠오르는 규칙을 끼워 넣을 여지가 없고 gherkin의 도메인 고도도 사람만큼 지키지는 못한다. 반대로 손으로 쓴 테스트는 느린 대신, 도메인 언어를 사람이 통제하고 작업하면서 생각난 축을 그때그때 보탤 수 있다.

다만 파이프라인이 훨씬 빠르고 편하다는 건 분명하다. 그러니 처음부터 완성품을 기대하기보다, 드러난 약점을 피드백으로 메워 나가는 쪽이 맞다고 본다. 플랫폼처럼 빠진 축은 정책 입력에 명시적으로 실어 주고, gherkin 고도는 rubric의 behavioral_altitude 피드백으로 조이면 된다. 파이프라인의 4단 루프 자체가 이런 교정을 위한 구조이니, 사람은 초안을 처음부터 쓰는 대신 입력과 rubric을 다듬는 쪽으로 힘을 옮기면 될 것 같다.