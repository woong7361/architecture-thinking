# TDD 하네스 v0 설계 (A-5 결정 문서)

> 목적: A-5 "요구사항 → 테스트" 파이프라인의 **결정된 v0 설계**. 무엇을 만들지 확정한다.
> 관계: `tdd-harness-design.md`(2스트림 outside-in 탐색안)를 구체 빌드안으로 좁힌 것.
> 이식 대상: `.codex/skills/blog-draft/pipeline/` (Gen·Critique·Eval·Refine·Validate + JSON 스키마
>            + YAML 루브릭 + runs/ 반복 수렴). `recruiting-harness-pipeline/`도 같은 골격의 다른 응용.
> 통과시킬 요구사항: `refund_design.md`(경계·실패 표), A-3 단위테스트, A-4 Gherkin.
> LLM 호출: **codex CLI** (`codex-cli 0.139.0` 설치 확인, claude CLI 없음) → `--provider codex`.
> 주의: 아래는 설계 확정안이지 실행 결과가 아니다. 로그·비교는 실행 후 채운다.

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

비교 축: **수렴 iteration 수 / 계약 churn(refine이 gherkin을 흔드나) / 생성 테스트 품질 /
손 A-3·A-4 대비**. → "번들이 왜 계약을 흔드는가"를 로그로 증명하면 수행내용 2가 강해진다.

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

## 1. 구조

### 1-1. 디렉터리 (재사용/신설 매핑 — 수행내용 1의 뼈대)

```
tdd-harness-pipeline/
  intake_to_input.*     [재사용→개조]  요구사항 NL → input.json (blog intake_to_input.py)
  runner.py             [재사용+신설]  루프·attempt·검증 골격 재사용 + --mode / y/n 동결 게이트 신설
  validate.py           [재사용+신설]  스키마 골격 그대로 + 금지패턴/행동-고도 가드(mode 분기) 신설
  prompts/
    gen_bundled.md  gen_contract.md  gen_unit.md   [신설]  ← 유일하게 갈라지는 프롬프트
    critique.md  eval.md  refine.md                [신설]  ← 3모드 공유
  rubrics/
    bundled.rubric.yaml  contract.rubric.yaml  unit.rubric.yaml   [신설]
  schemas/
    input|gen_output|critique_output|eval_output|draft|critique|eval|final.schema.json  [신설]
  runs/
    bundled/…   split/…    [재사용]  attempt별 로그 보존, 모드별 분리 (수행내용 3)
  frozen/       [신설]  split에서 승인된 계약 refund.feature (읽기 전용 승격)
```

runner에 `--mode {bundled, contract, unit}` 플래그를 두고, mode에 따라 gen 프롬프트·rubric·가드를 선택.

### 1-2. 루프

공통 (blog/recruiting 순서 — Eval 먼저, 떨어질 때만 Critique):

```
input.json ─► Gen ─► draft ─► Validate(스키마+금지패턴+가드) ─► Eval(rubric 가중)
                                                                   ├ PASS ─► final
                                                                   └ FAIL ─► Critique ─► Refine ─┐
  MAX_ITERATIONS 도달 → 중단, runs/에 수렴 여부 기록  ◄──────────────────────────────────────────┘
```

split의 계약 동결 메커니즘:

```
Run 1 (contract) NL 요구사항 ─► gherkin ─► ★runner 멈춤: y/n 승인★ ─► frozen/refund.feature (읽기전용)
                                                                            │
Run 2 (unit)     NL 요구사항 + frozen/refund.feature ─► 단위테스트 ◄─────────┘
                       (계약 재생성 안 함, 제약·재료로만 읽음)
```

- **동결 = 파일 승격**: Run 1 통과분을 `frozen/`으로 복사, 이후 수정 금지. Run 2는 입력으로만 받음.
- **사람 승인 게이트 (b 반자동)**: 자동 PASS만으로 동결하지 않는다. runner가 멈추고 `y/n`을 묻는다.
  사람이 계약을 소유한다(`tdd-ai-notes.md`: "사람이 게이트를 소유, 테스트는 닻").
- **Run 2 제약**: Gen/Refine 프롬프트에 "동결 계약과 모순 금지, 계약을 재서술 말고 계약이 남긴
  산술 세부(절사·마지막날 금액)를 단위로 채워라".

핵심: **input을 skill이 생성**한다(blog `intake_to_input.py` 재사용). Gen이 요구사항을 지어내지
않도록 refund_design §6 경계·실패 표를 input의 `boundary_cases[]`·`failure_cases[]`로 **명시적으로
실어** 넣는다. 이 목록이 coverage 채점을 결정적으로 만든다.

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

결정성 장치: **count 기반 채점**(coverage/unambiguity를 input 목록·금지패턴 개수로 검증) +
게이트 축(coverage·unambiguity 3.5, 나머지 3.0).

---

## 3. 각 Agent 프롬프트

### 3-1. 모든 Agent 공통 (짧게, 중복 제거)

- "이 산출물을 **당신이 만들지 않았다**" — 특히 Eval, 자기평가 편향 차단.
- 출력은 `schemas/*_output.schema.json` 계약만. **JSON 객체 하나**, 설명·코드블록·주석 금지.
- 메타(brief_hash/iteration/model 등)는 runner가 감싼다 → 모델이 출력하지 않는다.
- 금지 필드 명시(blog forbidden_fields 이식).

### 3-2. Eval 전용 채점 보정 (blog eval_system 문구 재사용)

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

blog-draft 격리 규칙 이식. "Gen·Critique·Eval 모두 AI라 블라인드 스팟이 상관관계를 가짐" 방어선.

| Agent | 받는 것 | 못 보는 것 (차단) | 왜 |
|---|---|---|---|
| **Gen** | `input`(요구사항+경계/실패 목록, split-unit은 +frozen 계약) | eval 점수, critique, validator 판정 | 백지에서 생성 |
| **Critique** | `input` + 현재 draft | **eval 점수·validator 판정** | 편향 방지("안 봤다고 가정") |
| **Eval** | `input` + draft + rubric | **critique** | 독립 채점("당신이 만들지 않았다") |
| **Refine** | `input` + 이전 draft + critique + refine_request(`weak_axes`,`contract_errors`) | **eval 원문·weighted_total** | 점수 맞추기(리워드 해킹) 방지 |

추가 순환 차단(파이프 밖): 사람 소유 루브릭 · 손 A-3/A-4를 **ground truth로 비교**(수행내용 2) ·
생성 테스트에 **결함 주입 1회**(Goodhart 방어).

---

## 5. 빌드 계획 & 범위

### Phase 1 — 스캐폴딩 & 제네릭화
- blog-draft/pipeline → `tdd-harness-pipeline/` 골격 복제. runner 루프·validate 골격 재사용.
- `--mode {bundled,contract,unit}` + `--provider codex` 배선. 재사용/신설 **매핑표** 산출(수행내용 1).
- **스모크 테스트**: 작은 input 1건 → gen 1회 → codex가 스키마 맞는 JSON을 실제로 뱉는지 확인.

### Phase 2 — split 먼저 (baseline, 권장 설계)
- `input.schema.json`(requirement + boundary_cases[] + failure_cases[]), intake(refund_design+A-4→input).
- contract: `contract.rubric.yaml`, gen_contract, 행동-고도 가드 → 실행 → **y/n 승인 → frozen/**.
- unit: `unit.rubric.yaml`, gen_unit(frozen 계약 제약) → 실행.

### Phase 3 — bundled variant 얹기
- `bundled.rubric.yaml`, gen_bundled, 가드 섹션 분기. 같은 요구사항으로 실행.

### Phase 4 — 비교 & 문서화 (수행내용 2·3)
- bundled vs split vs 손 A-3/A-4 **나란히 비교**.
- 각 run **MAX_ITERATIONS 수렴 횟수**·**계약 churn** `runs/`에서 기록.
- **결함 주입 1회** Red 확인. task1-5.md 제출물 작성.

### 범위
- **v0 포함**: 2모드(bundled/split), y/n 동결 게이트, 고도별 루브릭, 행동-고도 가드, 정보 차단, runs/ 로그.
- **v1로 미룸**: slow-loop(루브릭 자기개선), 계약 자동 회귀 판정 자동화.

---

## TODO

- [ ] blog-draft 골격 복제, `--mode`/`--provider codex` 배선, 재사용/신설 매핑표 (수행내용 1)
- [ ] codex 스모크 테스트 (인증·JSON 출력 확인)
- [ ] input.schema.json(boundary_cases·failure_cases) + intake
- [ ] contract/unit/bundled rubric 3종
- [ ] gen_contract/gen_unit/gen_bundled + 공유 critique/eval/refine
- [ ] 행동-고도 가드(mode 분기) validate 규칙
- [ ] y/n 동결 게이트 + frozen/ 승격
- [ ] split 실행 → 동결 → unit 실행 (Phase 2)
- [ ] bundled 실행 (Phase 3)
- [ ] bundled vs split vs 손 비교 + 수렴·churn 기록 + 결함 주입 (Phase 4)
