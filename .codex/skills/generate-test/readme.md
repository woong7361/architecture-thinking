# TDD 테스트 생성 파이프라인 설계 (v0)

---

## 0. 큰 결정: v0에서 두 방향을 만들어 A/B 비교한다

이 프로젝트 성격(뭐가 잘 되는지 실제로 대보고 배운다)에 맞춰, v0는 **한 코드베이스**로
두 토폴로지를 모두 돌려 비교한다.

- **A) bundled** — 한 run에서 gherkin + unit을 한 draft로 생성. 단일 루프.
- **B) split** — 계약(gherkin) run을 먼저 통과·동결한 뒤, 단위 run이 동결 계약을 제약으로 받음.
  (`tdd-harness-design.md`의 2스트림을 "같은 파이프 2회 실행"으로 구현)

```
A) bundled:  요구사항 ─► [gherkin+unit 한 draft] ─► eval(bundled rubric) ─► refine 루프
B) split:    요구사항 ─► [gherkin] ─►★y/n 동결★─► [unit + frozen 계약] ─► 각자 루프
```

비교 축: **수렴 iteration 수 / 계약 churn(refine이 gherkin을 흔드나) / 생성 테스트 품질**.
→ "번들이 왜 계약을 흔드는가"를 로그로 증명한다.

### 두 방향인데 왜 코드가 많이 안 늘어나나 — Gen만 갈라진다

4단 중 **Critique·Eval·Refine은 rubric-driven이라 고도에 무관**하다 → 3모드 공유.
실제로 갈라지는 건 **Gen 프롬프트 + rubric + 행동-고도 가드 범위** 뿐이다.

| 요소 | bundled | split-contract | split-unit | 공유 |
|---|---|---|---|---|
| runner / validate 골격 / codex 호출 / 스키마 래퍼 | ● | ● | ● | **100% 공유** |
| critique / eval / refine 프롬프트 | ● | ● | ● | **공유** |
| gen 프롬프트 | 두 섹션 생성 | gherkin만 | unit만(계약 제약) | 갈라짐 (+1) |
| rubric | bundled(5축) | contract(4축) | unit(4축) | 갈라짐 (+1) |
| 행동-고도 가드 | gherkin 섹션만 | 통째 적용 | 미적용 | mode 분기(작음) |

bundled를 추가로 얹는 비용 ≈ gen 프롬프트 1개 + rubric 1개 + 가드 분기 하나.
비싼 70%(runner·validate·codex·스키마)는 그대로 재사용.

**유일한 실질 비용은 코드가 아니라 실험·분석 면적**이다. 비교 매트릭스가
"생성 vs 손" → "bundled vs split vs 손"으로 늘어 run·로그가 1.5배가 된다.
→ 그래서 **split 먼저 끝까지 동작시키고, bundled를 얇은 variant로 얹는** 순서로 간다.

---

## 0-A. 결정 갱신 (2026-07-06) — input/intake/Gen/Eval 역할 재정의

초기 설계(§2·§5)는 refund_design §6의 경계·실패 표를 input의 `boundary_cases[]`·`failure_cases[]`로
실어 coverage를 결정적으로 만들려 했다. **이 방향을 폐기한다.** 판정기준(테스트 케이스)을 input에 넣으면
사람이 이미 경계·실패를 다 열거한 셈이라 **Gen이 생성할 게 없어진다**(목록 포맷팅으로 전락). 테스트 생성
하네스의 존재 이유 — 정책에서 경계값을 스스로 발굴 — 와 모순된다.

역할을 다시 긋는다:

```
사용자 도메인 입력 (정책/요구사항 원문, 느슨해도 됨)
   │
   ▼ intake  ── 정책이 미흡하면 질문. 단 "판정기준"이 아니라 "정책"을 보강한다.
   │            (예: "딱 7일째는 무료야 8일부터야?" ← 정책 모호성)
 input.json = 명확해진 정책 (테스트 케이스 없음)
   │
   ▼ Gen    ── 여기서 판정기준(gherkin/unit 케이스)을 생성
   ▼ Eval/Critique ── 정책 대비 coverage 판정
```

| 단계 | 다루는 것 |
|---|---|
| **input** | 사용자가 준 도메인 **정책/요구사항**(+원문 보존). 테스트 케이스 없음. |
| **intake** | 정책이 미흡하면 질문으로 **정책**을 보강(미정의 경계·규칙 우선순위 명확화). 판정기준은 만들지 않는다. |
| **Gen** | 정책 → **판정기준(테스트 케이스) 생성**. |
| **Eval/Critique** | 생성된 테스트가 정책이 함의하는 경계를 덮었는지 판정. |

- input은 **도메인 무관하게 일반적**이어야 한다. refund §6 표 형태를 스키마에 박지 않는다.
- 경계/실패를 별도 배열로 나누지 않는다(그건 한 도메인의 우연). input 형태:
  `requirement` + `source_material`(요약 금지, 필수) + 선택 `policy_rules`(정책을 **규칙**으로 분해 —
  케이스가 아니라 규칙) + 선택 `external_dependencies`(mock 판정 근거) + 선택 `constraints`.
  - `policy_rules` 예: "MANUAL > 7일무료 > PRORATION"은 **규칙**. "경과일 7↔8 경계 테스트"는 Gen이 그
    규칙에서 파생할 **케이스**. 규칙은 input, 케이스는 Gen 산출물.

### "count 기반 결정적 coverage"의 위치 — Eval이 자체 수행

결정성은 **고정된 외부 목록**이 아니라 **Eval의 채점 방법이 기계적**인 데서 온다. rubric이 Eval에게
다음 형식을 강제한다: (1) 정책에서 함의되는 경계·실패를 **열거**, (2) 생성 테스트에 각각 **매핑**,
(3) `덮은 수 / 열거한 수`로 **점수**. → "6/8 덮음, 누락: totalDays<=0, 마지막날" 같은 셀 수 있는 판정
(느낌 점수 아님).

- Eval 전용 coverage 체크리스트 아티팩트는 **만들지 않는다**(초안에서 잠깐 도입했다 폐기).
- Gen도 Eval도 **정책만** 본다. 같은 목록을 주입·공유하지 않아 §4 정보 차단과도 일치.
- Eval 자체 열거가 run마다 흔들릴 수 있는 신뢰성 문제는, **파이프 밖의 사람 피드백**(v1 problem.md)을
  외부 기준으로 대조해 보완한다(순수 기계 신호가 아님을 인정).

> 아래 §1·§2·§4·§5 중 `boundary_cases[]`/`failure_cases[]`를 input에 싣는다는 서술은 이 절로 대체된다.

---

## 1. 구조

### 1-1. 디렉터리 (재사용/신설 매핑)

```
.codex/skills/generate-test/pipeline/          # 설계 초안의 가칭 tdd-harness-pipeline/ → 실물 경로
  intake_to_input.py    [재사용→개조]  요구사항 NL → input.json
  runner.py             [재사용+신설]  루프·attempt·검증 골격 재사용 + --mode 신설 (동결 게이트는 skill 소유, runner 아님)
  validate.py           [재사용+신설]  스키마 골격 그대로 + 금지패턴/행동-고도 가드(mode 분기) 신설
  prompts/
    gen_bundled.md  gen_contract.md  gen_unit.md          [신설]  ← 유일하게 갈라지는 프롬프트
    critique_system.md  eval_system.md  refine_system.md  [신설]  ← 3모드 공유
  rubrics/
    bundled.rubric.yaml  contract.rubric.yaml  unit.rubric.yaml   [신설]
  schemas/
    input|gen_output|draft|critique|eval|final.schema.json         [신설]
    eval_output.{contract,unit,bundled}.schema.json               [신설]  ← 모드별 named/closed
  runs/
    bundled/<run_id>/…                 [재사용]  attempt별 로그 보존
    split/<group>/{contract,unit}/…    [재사용]  contract·unit 두 스트림을 한 그룹으로 묶어 보존
        └ eval.json(axis점수+rationale)·critique.json 구조화 보존 → v1 slow-loop 입력 (§5 참조)
        └ 승인된 contract 아티팩트(.../contract/<run_id>/artifact/*.feature)는 그 자리에서 chmod a-w로 잠금
                                       [신설]  freeze-in-place — 별도 frozen/ 폴더 없음
```

> **실물 반영 (2026-07-06):** 초기 설계는 가칭 `tdd-harness-pipeline/`에 별도 `frozen/` 폴더로 승인 계약을
> 복사·승격하려 했으나, 실제 구현은 `.codex/skills/generate-test/pipeline/`이고 동결은 **복사가 아니라
> 제자리 잠금(freeze-in-place)** 으로 바뀌었다. 아래 §1-2·§5의 서술도 이 결정에 맞춘다.

runner에 `--mode {bundled, contract, unit}` 플래그를 두고, mode에 따라 gen 프롬프트·rubric·가드를 선택.

### 1-2. 루프

공통 (Eval 먼저, 떨어질 때만 Critique):

```
input.json ─► Gen ─► draft ─► Validate(스키마+금지패턴+가드) ─► Eval(rubric 가중)
                                                                   ├ PASS ─► final
                                                                   └ FAIL ─► Critique ─► Refine ─┐
  MAX_ITERATIONS 도달 → 중단, runs/에 수렴 여부 기록  ◄──────────────────────────────────────────┘
```

split의 계약 동결 메커니즘:

```
Run 1 (contract) NL 요구사항 ─► gherkin ─► ★skill 멈춤: y/n 승인★ ─► 제자리 잠금(chmod a-w)
                                             runs/split/<group>/contract/<run_id>/artifact/*.feature
                                                                            │ (원문을 unit input JSON에 인라인)
Run 2 (unit)     NL 요구사항 + 인라인된 동결 계약 ─► 단위테스트 ◄────────────┘
                       (계약 재생성 안 함, 제약·재료로만 읽음)
```

- **동결 = 제자리 잠금(freeze-in-place)**: Run 1 승인분을 별도 `frozen/`으로 복사하지 않고, contract
  아티팩트를 **그 자리에서 `chmod a-w`로 잠근다**. 계약 원문은 unit input JSON에 인라인되어 Run 2가 소비하고,
  잠긴 아티팩트는 사람이 승인한 정본·감사 기준으로 그 자리에 남는다.
- **사람 승인 게이트 (반자동)**: 자동 PASS만으로 동결하지 않는다. 게이트는 **skill이 소유**(runner 아님) —
  skill이 멈추고 `y/n`을 묻는다. 사람이 계약을 소유한다(`tdd-ai-notes.md`: "사람이 게이트를 소유, 테스트는 닻").
- **Run 2 제약**: Gen/Refine 프롬프트에 "동결 계약과 모순 금지, 계약을 재서술 말고 계약이 남긴
  산술 세부(절사·마지막날 금액)를 단위로 채워라".

핵심: **input을 skill이 생성**한다(`intake_to_input.py`). input에는 **정책만** 담는다
(§0-A) — `requirement` + `source_material`(원문 보존) + 선택 `policy_rules`·`external_dependencies`.
Gen이 정책 밖 요구사항을 지어내지 않도록 `source_material`을 요약 없이 싣고, 판정기준(테스트 케이스)은
Gen이 생성한다. coverage 결정성은 Eval의 열거→매핑→카운트 방법에서 온다(§0-A).

---

## 2. 루브릭 — 고도별로 분리, 1/3/5 결정적 기준

3점을 기준(평균)으로, 1·3·5를 "느낌"이 아니라 **셀 수 있게** 쓴다.
split은 contract/unit 4축씩, bundled는 둘을 합친 5축(비교 대상이라 의도적으로 어정쩡함을 남김).

### 2-1. contract.rubric.yaml (split Run 1)

| 축 | w | 1점 | 3점(기준) | 5점 |
|---|---|---|---|---|
| **coverage** | 0.30 | Happy만, 경계·실패 시나리오 0개 | Happy+일부 경계, `failure_cases` 절반 이상 누락 | boundary/failure 목록 각 항목이 시나리오 1개 이상 대응 |
| **unambiguity** | 0.30 | Then에 'should work'·빈 기대값 | 대부분 구체값이나 일부 Then 정성적 | 모든 단언이 구체 기대값(금액·상태·예외타입)으로 결정적 |
| **behavioral_altitude** | 0.20 | 클래스/메서드 등 구현 세부 노출 | 대체로 도메인 언어이나 일부 구현 용어 | 구현 세부 미노출, 도메인 언어로만 서술 |
| **independence** | 0.20 | 앞 시나리오 상태·순서에 의존 | 대체로 독립이나 공유 픽스처 영향 여지 | 각 시나리오 자기 입력만으로 실행, 순서·공유상태 의존 0 |

### 2-2. unit.rubric.yaml (split Run 2)

| 축 | w | 1점 | 3점(기준) | 5점 |
|---|---|---|---|---|
| **coverage** | 0.30 | Happy만, 경계·예외 0개 | 일부 경계, 목록 절반 이상 누락 | boundary/exception 목록 각 항목이 테스트 1개 이상 대응 |
| **unambiguity** | 0.25 | 빈 기대값·모호 단언 | 대부분 구체값이나 일부 범위값 | 모든 단언이 구체 기대값으로 결정적 검증 |
| **mock_discipline** | 0.25 | 순수 계산·상태전이를 Mock으로 감쌈 | 외부 의존은 Mock하나 일부 과한 verify/stub | 외부 의존(PG/DB)만 Mock, 순수 로직 실제 객체 상태검증 |
| **executability** | 0.20 | 도메인에 없는 개념 참조·구현 불가 | 대체로 구현 가능하나 일부 시그니처 모호 | 명확 입력·기대값으로 단위테스트 즉시 구현 가능, FIRST |

### 2-3. bundled.rubric.yaml (bundled)

2-1·2-2를 합친 5축: `coverage(0.25)·unambiguity(0.25)·behavioral_altitude(0.15,
gherkin 섹션만)·mock_discipline(0.15, unit 섹션만)·independence/executability(0.20)`.
→ 섹션마다 어정쩡하게 걸리는 문제를 **일부러 남겨** split과 대비한다(비교 관찰 대상).

### 2-4. thresholds (공통 형태)

```yaml
thresholds:
  min_total: 3.8
  min_axis: { coverage: 3.5, unambiguity: 3.5, <나머지>: 3.0 }   # coverage·unambiguity가 게이트 핵심
```

결정성 장치: **count 기반 채점** — coverage는 Eval이 정책에서 함의 경계를 열거→생성 테스트에 매핑→
`덮은 수/열거 수`로 채점(§0-A, input에 케이스를 싣지 않는다), unambiguity는 금지패턴 개수로 검증 +
게이트 축(coverage·unambiguity 3.5, 나머지 3.0).

---

## 3. 각 Agent 프롬프트

### 3-1. 모든 Agent 공통 (짧게, 중복 제거)

- "이 산출물을 **당신이 만들지 않았다**" — 특히 Eval, 자기평가 편향 차단.
- 출력은 `schemas/*_output.schema.json` 계약만. **JSON 객체 하나**, 설명·코드블록·주석 금지.
- 메타(brief_hash/iteration/model 등)는 runner가 감싼다 → 모델이 출력하지 않는다.
- 금지 필드 명시(forbidden_fields).

### 3-2. Eval 전용 채점 보정

> 5점은 매우 드물다. 평균 3.0 기준으로 채점한다. 4점 이상은 그 축에서 뚜렷한 완성도·구체성이
> 있을 때만. 2점대는 실패가 아니라 개선 여지가 분명한 상태다.

### 3-3. 역할 · 규칙 (Gen만 mode별로 갈라짐)

| Agent | Role | must-do | must-not |
|---|---|---|---|
| **Gen (공통)** | 시니어 테스트 엔지니어 | Happy+Unhappy(경계·실패·거절) 모두 · 구체 기대값 · 경계는 안/밖 짝(7일↔8일, 0일↔1일) · 각 테스트 독립 | 'should work' 류 모호표현 · 입력에 없는 요구사항·수치 생성 · 자기점수/판정 출력 |
| ┗ gen_contract | (gherkin) | G/W/T 구조, 도메인 언어 | Gherkin에 클래스/메서드명 노출(행동-고도) |
| ┗ gen_unit | (JUnit, 계약 제약) | 순수로직 실제객체, 외부의존만 Mock · **frozen 계약과 모순 금지** | 순수로직 Mock · 계약 재서술 |
| ┗ gen_bundled | (두 섹션) | 위 둘을 한 draft의 `gherkin[]`·`unit[]`로 | 섹션 혼동 |
| **Critique (공유)** | 시니어 QA (재작성·채점 안 함) | weaknesses 3개 목표(빠진 경계값/누락 실패케이스/모호한 Then/Mock 남용) · 각 약점 `issue·why_it_matters·suggestion·severity` 분리 · strengths 분리 | 초안 재작성 · 숫자점수·PASS/REJECT · input에 없는 요구사항 추가 지시("설계 문서에 있다면" 조건) |
| **Eval (공유)** | 엄격 심사관 (창작자 아님) | 전달받은 rubric 축만으로 채점 · 각 축 점수+근거 1줄 · weighted_total 계산 | 재작성 · 최종 판정(PASS/REJECT는 validator·runner 책임) · 없는 사실 상상 보완 |
| **Refine (공유)** | 퇴고자 | critique 약점·`weak_axes`·`contract_errors` 우선 반영 · 원문 사실·의도·제약 유지 · strengths 유지 | 점수 맞추기 · 입력에 없는 사실 생성 · 수정 설명/점수 출력 |

---

## 4. Agent별 정보 차단 (순환성 끊기)

격리 규칙: "Gen·Critique·Eval 모두 AI라 블라인드 스팟이 상관관계를 가짐" 방어선.

| Agent | 받는 것 | 못 보는 것 (차단) | 왜 |
|---|---|---|---|
| **Gen** | `input`(정책: requirement+source_material+policy_rules, split-unit은 +frozen 계약) | eval 점수, critique, validator 판정, **coverage 열거 목록** | 백지에서 케이스 발굴 |
| **Critique** | `input` + 현재 draft | **eval 점수·validator 판정** | 편향 방지("안 봤다고 가정") |
| **Eval** | `input` + draft + rubric | **critique** | 독립 채점("당신이 만들지 않았다") |
| **Refine** | `input` + 이전 draft + critique + refine_request(`weak_axes`,`contract_errors`) | **eval 원문·weighted_total** | 점수 맞추기(리워드 해킹) 방지 |

추가 순환 차단(파이프 밖): 사람 소유 루브릭 · **사람 피드백을 ground truth로 대조**(v1 problem.md).

---

## 5. 빌드 계획 & 범위

### Phase 1 — 스캐폴딩 & 제네릭화
- `.codex/skills/generate-test/pipeline/` 골격 구성. runner 루프·validate 골격 공유.
- `--mode {bundled,contract,unit}` + `--provider codex` 배선. 재사용/신설 **매핑표** 산출.
- **스모크 테스트**: 작은 input 1건 → gen 1회 → codex가 스키마 맞는 JSON을 실제로 뱉는지 확인.

### Phase 2 — split 먼저 (baseline, 권장 설계)
- `input.schema.json`(정책만: requirement + source_material + 선택 policy_rules/external_dependencies — §0-A),
  intake(정책 미흡 시 정책 보강 → input).
- contract: `contract.rubric.yaml`, gen_contract, 행동-고도 가드 → 실행 → **y/n 승인 → 아티팩트 제자리 잠금(freeze-in-place)**.
- unit: `unit.rubric.yaml`, gen_unit(동결 계약 제약) → 실행.

### Phase 3 — bundled variant 얹기
- `bundled.rubric.yaml`, gen_bundled, 가드 섹션 분기. 같은 요구사항으로 실행.

### Phase 4 — 비교 & 문서화
- bundled vs split **나란히 비교**.
- 각 run **MAX_ITERATIONS 수렴 횟수**·**계약 churn** `runs/`에서 기록·문서화.

### 범위
- **v0 포함**: 2모드(bundled/split), y/n 동결 게이트, 고도별 루브릭, 행동-고도 가드, 정보 차단, runs/ 로그.
- **v1로 미룸**: slow-loop(루브릭 자기개선), 계약 자동 회귀 판정 자동화.

#### slow-loop를 v0에서 빼는 이유

fast loop이 남긴 run 로그를 모아 **파이프라인 자체(루브릭·프롬프트·코드)를 고치는 제안**을
만드는 두 번째 느린 루프(slow-loop)는 여기엔 의도적으로 넣지 않는다.

1. **데이터가 없다.** slow-loop 트리거는 `pending ≥ 5` run 누적이다. v0는 run을 처음 돌려보는 단계라
   집계할 로그 자체가 없다. slow-loop는 fast loop이 먼저 돌아 로그가 쌓인 뒤에 오는 층이다.
2. **v0의 목적이 다르다.** 이 하네스 v0의 실험 목표는 루브릭 자기개선이 아니라 **bundled vs split
   비교**(Phase 4)다. slow-loop는 그 비교 결과가 나온 *다음*에 "그래서 뭘 고칠까"를
   자동화하는 층이라 순서상 뒤다.
3. **비용.** §0에서 밝혔듯 v0의 유일한 실질 비용은 실험·분석 면적이다. slow-loop를 얹으면 proposal
   rubric·proposer stage·analyze 스크립트가 통째로 추가돼 v0 범위를 넘는다.

#### 단, 로그는 v0에서 집계 가능한 형태로 남긴다 (slow-loop의 씨앗)

slow-loop 본체는 v1로 미루되, **그 입력이 되는 run 로그 포맷은 v0에서 미리 맞춘다.** slow-loop의
`analyze_runs.py`가 로그를 긁으려면 각 run이 **axis 점수 + rationale**를 구조화해 남겨야 한다.
v0의 `runs/{bundled,split}/…`도 attempt별로 다음을 남겨, v1에서 slow-loop를 얹을 때 로그 포맷을
갈아엎지 않게 한다.

- `eval.json`: axis별 점수 + **근거 1줄(rationale)** + `rubric_name`(예: `contract:v0`) + weighted_total
- `critique.json`: weaknesses(issue/why/severity) — 같은 지적 반복 여부를 나중에 집계할 수 있게
- attempt 메타: `mode`, `iteration`, terminal_reason(PASS/FAIL/MAX_ITER)
- 이 셋만 있으면 v1의 analyze가 "같은 axis 반복 미달 / critique 반복 지적"을 그대로 집계할 수 있다.

---

## TODO

- [ ] 파이프라인 골격 구성, `--mode`/`--provider codex` 배선, 재사용/신설 매핑표
- [ ] codex 스모크 테스트 (인증·JSON 출력 확인)
- [ ] input.schema.json(정책만: requirement+source_material+선택 policy_rules) + intake(정책 보강)
- [ ] contract/unit/bundled rubric 3종
- [ ] gen_contract/gen_unit/gen_bundled + 공유 critique/eval/refine
- [ ] 행동-고도 가드(mode 분기) validate 규칙
- [ ] y/n 동결 게이트 + 아티팩트 제자리 잠금(freeze-in-place, skill 소유)
- [ ] split 실행 → 동결 → unit 실행 (Phase 2)
- [ ] bundled 실행 (Phase 3)
- [ ] bundled vs split 비교 + 수렴·churn 기록 (Phase 4)
