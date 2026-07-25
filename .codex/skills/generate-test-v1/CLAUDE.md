# generate-test 하네스 — AI 작동 규칙

> 이 파일은 상위 `AGENTS.md`·`task1/CLAUDE.md`를 상속한다. `.codex/skills/generate-test-v1/`
> 안에서 작업할 때 우선 적용된다. **v1 결정(spec-first)**: [docs/v1-spec-first-design.md](docs/v1-spec-first-design.md).
> 아래 규칙 중 v0(테스트 코드·split·unit·bundled) 서술은 v1에서 폐기됐다 — 산출은 **명세 문서 2종**뿐이다.

## 이게 뭔가

요구사항(NL) → **명세 문서**(boundary feature / 도메인 규칙 예시표)를 **생성하는** 4단 루프 하네스다.
runner·validate·codex 호출·스키마 래퍼를 공유하는 파이프라인 골격 위에서 명세 생성용으로 구성한다.
**테스트 코드(step/fake/단위)는 만들지 않는다** — feature는 시그니처가 아니라 경계 행동만 고정해 인터페이스
설계를 사람의 사이클로 미룬다. 목적은 **"설계의 결정론적 명세"** 를 뽑는 것이다(실행 게이트는 사람이 바인딩 시 완성).

루프(공통): `input → Gen → draft → Validate(스키마+가드) → Eval(rubric) → PASS면 final / FAIL면 Critique → Refine → 반복`.
`--max-iterations` 도달 시 중단하고 `runs/`에 수렴 여부를 남긴다.

## 핵심: mode 하나가 세 가지를 함께 고른다

`runner.py --mode {contract,rules}` 플래그가 **gen 프롬프트 · rubric · runs 그룹 · eval 스키마**를 함께 선택한다.
**critique / eval / refine 프롬프트, validate 골격, codex 호출, 스키마 래퍼는 모드와 무관하게 공유된다.**
갈라지는 건 Gen 쪽 + rubric + (행동-고도 가드 범위)뿐이다.

| mode | 무엇을 생성 | gen 프롬프트 | rubric | runs 그룹 |
|---|---|---|---|---|
| `contract` | boundary feature(Gherkin, When=Inbound Port 1:1) | `prompts/gen_contract.md` | `rubrics/contract.rubric.yaml` | `runs/<group>/feature/` |
| `rules` | 도메인 규칙 예시표(입력→출력 표) | `prompts/gen_rules.md` | `rubrics/rules.rubric.yaml` | `runs/<group>/rules/` |

`<group>`은 input의 `group` 필드(미지정 시 `brief_hash`). feature와 그 파생 rules가 같은 group을 공유해
`runs/<group>/{feature,rules}/`로 한 폴더에 모인다. 실행 위치가 input에서 나오므로 각 모드는 플래그 없이
독립 실행된다(rules 단독 실행도 제 그룹으로 들어감). rules input의 group은 intake `--group`으로 원본
feature의 group(=feature `brief_hash`)을 물려받는다. `resolve_runs_dir`가 `runs/<group>/<mode-folder>/`를 만든다.

- `contract`·`unit`은 같은 **split 실험**의 두 스트림이라 **하나의 그룹 폴더**로 묶는다:
  `runs/split/<group>/contract/` 와 `.../unit/` (group = contract run_id `<오늘>_<contract_hash>`).
  `MODE_RUN_GROUP`은 mode→`split`/`bundled` 최상위 그룹만 정하고, contract/unit을 한 그룹에 묶는
  하위폴더(`<group>/{contract,unit}`)는 **skill이 `--runs-dir`로 넘긴다**(오케스트레이션은 skill 소유).
  skill이 `--runs-dir`을 안 넘기면 `runs/split/` 바로 밑에 date_hash 폴더로 흩어진다.
- 기본값은 `contract` (Phase 2 baseline이 여기서 시작).
- 매핑은 [runner.py](pipeline/runner.py)의 `MODE_GEN_PROMPT` / `MODE_RUBRIC` / `MODE_RUN_GROUP`에 있다.
  이 셋을 고치면 모드 동작이 한 곳에서 바뀐다.
- **산출물은 파일 매니페스트**: Gen/Refine 출력은 `files: [{path, content}]`(gen_output.schema).
  unit은 대상 클래스마다 별도 `.java`, contract는 `.feature`, bundled는 둘 다. PASS 시
  `materialize_artifacts`가 각 파일을 `<runs-dir>/<run_id>/artifact/<path>`로 승격한다(split이면
  `runs/split/<group>/{contract,unit}/<run_id>/artifact/<path>`; 경로는
  `sanitize_rel_path`로 `..`·절대경로 차단). draft/final의 `content`는 매니페스트를
  `// ===== FILE: <path> =====` 헤더로 이어붙인 **텍스트 뷰**로, critique/eval/guard가 이걸 읽는다
  (`synthesize_content`). 즉 stage들은 mode 무관하게 content 문자열 하나만 보면 된다.

### 경로 override 규칙

- `--rubric <path>` 명시 → mode 기본 rubric을 덮어씀. 미지정 → `rubrics/<mode>.rubric.yaml`.
- `--runs-dir <path>` 명시 → 그 경로 사용. 미지정 → mode 그룹으로 자동 결정.
- gen 프롬프트는 override 없이 **항상 mode가 결정**한다(모드의 정의 그 자체이므로).

## split의 계약 동결 (반자동 게이트) — 설계됨, 아직 미구현

split은 두 run이다. Run 1(contract)이 통과하면 **skill이 멈추고 `y/n` 승인을 묻고**, 승인분을
**제자리에서 읽기전용으로 잠근다(freeze-in-place)** — 별도 `frozen/` 폴더로 복사하지 않고 contract 아티팩트
(`runs/split/<group>/contract/<run_id>/artifact/*.feature`)를 `chmod a-w`로 고정한다. Run 2(unit)는 그 동결
계약을 **입력·제약으로만** 받고 재생성하지 않는다. **사람이 계약을 소유한다**(자동 PASS만으로 동결하지 않는다).

> 동결 = 복사가 아니라 **제자리 잠금**. 계약 원문은 unit input JSON에 인라인되어 unit run이 소비하고,
> 잠긴 아티팩트는 사람이 승인한 정본·감사 기준으로 그 자리에 남는다. skill-root의 전역 `frozen/` 폴더는 없앴다.

## 지금 되어 있는 것 / 다음 단계

되어 있음:
- 파이프라인 골격 구성(slow-loop 계열 전부 제외), `run_draft.py` slow-loop 잔재 제거.
- `--mode` 배선: mode → gen 프롬프트 · rubric · runs 그룹 · codex eval 스키마 선택. `--provider codex`는 골격에 이미 있음.
- **input = 정책만** (설계 §0-A). `schemas/input.schema.json` + `intake_to_input.py` 교체 완료(정책 플래그).
  판정기준(테스트 케이스)은 input에 없다 — Gen 산출물이다.
- **eval 축-불가지론화**: `eval_output.schema.json`·`eval.schema.json`은 축을 열거하지 않고, 축 정확성은
  `validate_eval_contract`가 rubric으로 대조. codex `--output-schema`만 mode별 named/closed
  (`eval_output.<mode>.schema.json`)로 강제. → 검증 1벌, 생성 제약만 mode별.
- `rubrics/contract.rubric.yaml`(5축, coverage는 Eval 열거→매핑→카운트) + `eval_output.contract.schema.json`.
  - **`interaction_hazard_coverage` 축**(신설): 값 경계와 별개로, outbound 협력의 **비멱등-재시도·동시요청 hazard**
    시나리오 완전성을 게이트한다. 값 삼각측량이 못 보는 "호출 이력에 따라 갈리는 결함"(이중처리·관측≠실제 재시도)을 잡는다.
  - **`boundary_fidelity` 카브아웃**: outbound의 **누적/net 결과 상태** 단언은 A(도메인 상태)로 인정 — 캡 아님.
    순서·횟수·verify·내부기본값만 캡한다. hazard를 net 상태로 단언하는 것과 이 축의 충돌을 없앤 correctness 수정
    (실측: 카브아웃 전 hazard 시나리오가 bf 3에 캡돼 게이트 미통과 → 카브아웃 후 bf 5로 PASS).

- `prompts/gen_contract.md` — 정책 → gherkin 계약 생성. TDD 개념을 생성 절차로 심음(삼각측량·단언 우선·
  out-in·빨강 가능성·순차 리팩터링). **contract 한 줄이 실제 codex run에서 end-to-end PASS 확인됨(4.59/5).**
- 공유 프롬프트(`eval_system`·`critique_system`·`refine_system`) 문구를 글쓰기→테스트 생성으로 개조 완료.
- **모호-단언 금지패턴 가드**(`validate.check_forbidden_assertions`, 한국어 부사+동작 조합). 위반은 하드 중단이
  아니라 eval을 REJECT시켜 `contract_error`로 refine 루프에 전달 → MAX_ITER까지 자가 수정.

- **unit 모드**: `prompts/gen_unit.md`(JUnit5+Mockito+AssertJ, Mock 규율·frozen 계약 제약) +
  `rubrics/unit.rubric.yaml`(coverage·unambiguity·mock_discipline·executability) + `eval_output.unit.schema.json`.
  경로·축 일치 로컬 검증 완료(실제 codex run은 아직).

- **SKILL.md + 동결 게이트 정의**: 동결 게이트는 **skill이 소유**(runner 아님). SKILL.md에 split 오케스트레이션
  (contract PASS → gherkin 제시 → y/n → 아티팩트 제자리 잠금 → unit input에 주입 → unit run) 명문화.
  intake에 `--frozen-contract-file` 추가, unit input에 `frozen_contract` 주입 검증 완료.

- **bundled 모드**(Phase 3): `prompts/gen_bundled.md`(두 섹션을 `=== GHERKIN ===`/`=== UNIT ===` 구분자로 한 draft에) +
  `rubrics/bundled.rubric.yaml`(5축, behavioral_altitude=gherkin섹션만·mock_discipline=unit섹션만) + `eval_output.bundled.schema.json`.
  일부러 어정쩡하게 남겨 split과 대비(계약 churn 관찰). 경로·축 일치 로컬 검증 완료.

**세 모드(contract/unit/bundled) 모두 파일·배관 완비.** 아직 없음(Phase 4):
1. 실제 codex로 3방향 관통(split: contract→동결→unit / bundled) — 실행/y/n은 skill 런타임.
2. **Phase 4 비교**: bundled vs split 나란히, MAX_ITER 수렴 횟수·계약 churn을 `runs/`에서 기록·문서화.
3. (선택) 클래스/메서드명 노출 하드 가드 — 지금은 rubric behavioral_altitude 축으로만 잡음.

> 지금 `--mode contract` 실제 run은 gen→critique→eval→PASS까지 돈다(codex 호출·비용 발생).
> 완주 검증은 step 완료 후 몰아서 — [[codex-run-pacing]].

## 이 하네스에서 작업할 때 지킬 것

- **가로로 다 만들지 말 것.** 두 모드를 동시에 채우지 말고, `contract`(boundary feature)를 끝까지
  관통·검증한 뒤 `rules`로. 한 모드를 끝까지 검증하고 다음으로.
- **공유 3단(critique/eval/refine)은 모드별로 복제하지 말 것.** 갈라지는 건 Gen·rubric·가드뿐이다.
  공유 스테이지를 모드별로 나누고 싶어지면 먼저 왜 rubric-driven으로 안 되는지 근거를 남긴다.
- **로그 포맷을 깨지 말 것.** `runs/`의 attempt별 `eval.json`(axis 점수+rationale+rubric_name+weighted_total),
  `critique.json`(weaknesses), 메타(mode/iteration/terminal_reason)는 v1 slow-loop의 입력이다.
  이 구조를 유지하면 v1에서 로그를 갈아엎지 않아도 된다.
- **Gen이 요구사항을 지어내지 않게 할 것.** input에는 **정책만** 담는다(케이스 목록 없음) — 경계·실패
  케이스는 Gen이 정책에서 발굴한다. coverage 결정성은 Eval의 열거→매핑→누락 카운트에서 온다(설계 §0-A).
- **feature는 경계, 산식은 rules.** boundary feature에 계산 산식을 박지 않는다(고도 침식). 산식·구체 값은
  `rules` 문서로 분리한다. When은 Inbound Port 1:1, Then은 경계 관찰 결과만. **금지**: 호출 순서·횟수·verify·
  내부상태·내부 표현 기본값. **허용**: outbound의 누적/net 결과 상태(반영 총량·누계) — 이는 상호작용이 아니라
  도메인 상태 관찰이라 hazard 시나리오의 정당한 단언 형태다(bf 카브아웃).
- **정보 차단(순환성 끊기)을 유지할 것.** Gen은 eval/critique를 못 본다, Critique는 eval 점수·validator
  판정을 못 본다, Eval은 critique를 못 본다, Refine은 eval 원문·weighted_total을 못 본다. 설계 §4 표 준수.
