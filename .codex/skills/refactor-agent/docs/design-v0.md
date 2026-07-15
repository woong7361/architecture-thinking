# B-6 설계 문서 (v0) — SOLID 위반 탐지 + 리팩토링 제안 파이프라인

> 상태: **가설(밑그림)**. 확정 스펙 아님. 구현·테스트가 검증하며, 뒤 단계에서 앞 단계로
> 되돌아오는 것을 막지 않는다(task2/CLAUDE.md BDUF 경계). 이름 `refactor-agent`도 잠정.

> **공용 스킬 원칙:** 이 스킬은 특정 코드베이스·과제에 묶이지 않는 **범용 SOLID 리팩토링 파이프라인**이다.
> 스킬 자산(참조 카탈로그·프롬프트·루브릭·runner)은 **target 무관**을 유지한다. B-2/B-5(task2)는 이 스킬을
> **처음 검증하는 target 예시**일 뿐이며, target별 데이터(골든 진단 앵커·경계 테스트 셀렉터·baseline·비교표)는
> 스킬에 넣지 않고 **run 입력**으로 공급한다. 이 문서에서 B-2/B-5를 드는 곳(§8 등)은 전부 "적용 예시"다.

## 0. 이 파이프라인이 무엇인가 (범위)

**행위 검증된 리팩토링 후보 생성기 + 3중 검증** 파이프라인이다. 코드를 실제로 만든다 —
Gen이 리팩토링된 파일 매니페스트를 내고, Validate가 worktree에 적용·테스트해 GREEN이면
그 후보는 **행위를 안 깬다는 게 증명된** 것이다. (행위 보존은 텍스트만으로 증명 불가라, 검증하려면
코드를 만들어 돌릴 수밖에 없다 — 설계가 코드 생성을 강제한다.)

핵심 구분은 "코드를 만드느냐"가 아니라 **무엇이 증명됐느냐**다 — **검증됨 ≠ 채택됨**:

- 테스트 GREEN이 증명하는 것 = **행위 보존뿐.** "동작을 안 깬다"까지.
- 증명하지 않는 것 = **설계가 옳음.** 과설계·엉뚱한 방향일 수 있다. 테스트 통과 ≠ 좋은 설계.
- 그래서 파이프라인은 코드를 만들지만 **"이게 네가 머지할 리팩토링"이라고 결정하지 않는다.**
  채택·설계 판단은 사람 몫이고, 후보는 B-5 수작업과 **나란히 비교당한다**(수행내용 2).
- "제안"은 프로즈가 아니라 **동작하는 후보 코드 + 행위 보존 증거**를 뜻한다.

> **주의(게이트의 정확한 정의 — §5 Step 4):** "모든 테스트 GREEN"이 게이트가 **아니다.**
> 리팩토링은 구조를 옮기므로 **구조 결합 단위 테스트는 당연히 깨지고(컴파일조차 실패), 순서의존
> 테스트는 거짓 RED**를 낸다. 그걸 게이트로 걸면 좋은 리팩토링을 거부한다. 게이트는 **안정된 공개
> 경계에 고정해 리팩토링 전 원본에서 동결한 특성화(행위) 테스트만** = 1-1의 contract 층이지 unit
> 층이 아니다. (B-5 안전망 커밋 `f97c1a7`이 Cucumber 인수 6시나리오였던 것과 동일.)

---

## 1. 재사용 vs 신규 (수행내용 #1 — 어디를 재사용했고 무엇을 새로 만들었나)

generate-test 하네스(`.codex/skills/generate-test/`)의 골격을 재사용한다. B-6는 그 골격을
**형제 스킬**로 가져와 도메인 부품만 교체한다(새 `mode`가 아님 — 이유 §6).

| 구성요소 | 재사용 / 신규 | 근거 |
|---|---|---|
| 루프 제어 구조 (`input→Gen→draft→Validate→Eval→PASS/refine→반복`, `--max-iterations`) | **재사용** (`runner.py` 골격) | 4단 + 수렴 게이트 동일 |
| 스키마 검증 + 역할경계 금지필드 (`validate.py::validate_file`, `*_FORBIDDEN_FIELDS`) | **재사용** | Gen이 점수 못 냄, Critique가 판정 못 냄 등 |
| **금지패턴 가드 → contract_error → refine 루프** (`check_forbidden_assertions`) | **재사용(패턴만 교체)** | "should be cleaner" 류를 이 배관으로 거른다 |
| rubric YAML 포맷 + 로더 (axes/weight/scale 사다리/thresholds min_total·min_axis) | **재사용** | 결정적 채점 틀 |
| eval 축-불가지론 스키마 + `validate_eval_contract`(축 집합 대조) | **재사용** | 축은 rubric이 정의, 검증 1벌 |
| 파일 매니페스트 → `materialize_artifacts`/`sanitize_rel_path`(경로 이탈 차단) | **재사용** | 리팩토링된 코드를 worktree에 떨굴 때 그대로 |
| `RunContext` lineage + `runs/` 로그 구조(eval.json 축점수+rationale, critique.json, terminal_reason) | **재사용** | 로그 보존·과설계 사례 기록(수행내용 #3) |
| LLM 클라이언트 포트(`llm_client` codex/claude) | **재사용** | 공급자 교체 지점 |
| **스멜 24 카탈로그 + A/B/C 결정 게이트** (`references/smell-solid-map.md` 1·2부) | **복사**(파울러 3장 카탈로그) | 스킬 자립 위해 인용 아닌 복사 |
| **SOLID 매핑 레이어** (같은 파일 3부) | **신규** | 스멜↔SOLID 연결(DIP≠ISP, OCP↔B게이트) |
| target 골든 진단 앵커 (예: B-2 kata) | **run 입력**(스킬 밖) | 과제별 데이터 — 공용 참조에 안 넣음. Eval 회귀 기준으로 공급 |
| `diagnose_refactor.md` (코드 → 위반+리팩토링 제안, **설계만**) | **신규** | 진단(판단) 책임 |
| `implement_refactor.md` (승인 제안 → 리팩토링된 파일 매니페스트) | **신규** | 구현(변환) 책임 — 진단과 분리 |
| `refactor.rubric.json` (4축: 진단정확도·변경최소성·행위보존위험·테스트용이성) | **신규** | B-6가 지정한 평가축 |
| `critique_refactor.md`·`eval_refactor.md` (제안+코드 리뷰·채점) | **신규** | generate-test 공유 critique/eval은 테스트용 |
| `refine_refactor.md` (설계 refine) | **신규·보류** | 구현 refine만 배선(§9). 설계 루프는 미배선 |
| **`behavior_gate.py` — worktree 적용 + 경계 테스트 + GREEN/RED/폐기** | **신규(핵심)** | generate-test엔 없음. Validate를 사실 층까지 확장 |
| 입력 스키마(정책 → **코드 스냅샷 + 테스트 명령**) | **신규** | input이 정책이 아니라 대상 코드 |

> **v0 재사용 방식(정직하게):** 진짜 도메인-불가지론인 조각(스키마검증·금지패턴 프레임워크·
> rubric 로더·RunContext·매니페스트 승격·llm_client)은 **import**로 재사용한다. 루프 골격은
> mode 디스패치가 테스트생성에 묶여 있어 깔끔한 import가 안 되므로 **복사 후 개조**하고 출처를
> 주석으로 남긴다. "공유 코어 추출"은 **소비자가 3개째 생길 때까지 보류**(YAGNI — §6 과설계 표시).

---

## 2. 협력 흐름 (RDD ① — 유스케이스의 메시지 흐름)

> 설계의 driver는 협력이다. 객체/데이터부터 시작하지 않는다.

유스케이스: *"이 코드를 안전하게 리팩토링하고 싶다. 뭘 어떻게 고쳐야 안전한가?"*

```
[사용자] --코드/폴더 + 1-1 테스트 위치--> [Orchestrator]
Orchestrator --"진단하고 제안하라"--------> [Diagnose]   → 위반 + 제안(설계, 코드 없음)
Orchestrator --"제안을 코드로 실현하라"------> [Implement]  → 리팩토링된 파일 매니페스트
Orchestrator --"행위 보존인가"-------------> [Validate]   → worktree 테스트 GREEN/RED
     RED → [Refine implement] → 다시 Implement
     GREEN ↓ (행위 보존이 품질 리뷰의 전제)
Orchestrator --"과설계/누락 + 채점"---------> [Critique ∥ Eval]  (병렬 — 서로 못 봄)
     Critique → 지적사항(제안+코드)   /   Eval → 축별 점수 + rationale
     Eval 미달 → [Refine] (Eval이 진단/구현 중 귀속) → 다시 Diagnose 또는 Implement
     통과 → final(제안+코드+점수+증빙)
```

**두 층으로 나뉜다:** ① 행위 게이트(Validate)가 **먼저** — 행위를 안 깨는 코드만 품질 리뷰할 값이 있다.
② 그 뒤 Critique·Eval이 **구현된 코드까지 보고** 판단(변경 최소성·테스트 용이성·행위 위험 4축 중 3축이 실제 diff를
요구). Critique와 Eval은 **서로 못 보므로(정보 차단) 병렬 실행**한다.

메시지가 먼저다. 각 메시지를 "그 정보를 가장 잘 아는 객체"에게 할당하면 아래 책임이 태어난다.

---

## 3. 책임 배분 (RDD ② — 정보 전문가로 한 줄 정당화)

| 객체(stage) | 책임 (변경 이유 하나 = SRP) | 왜 이 객체인가(정보 전문가) |
|---|---|---|
| **Diagnose(진단)** | 코드에서 SOLID 위반·smell을 진단하고 리팩토링 **제안**을 낸다(설계만, 코드 없음) | "무엇이 왜 잘못됐고 어떤 기법으로 고치나"를 아는 지점 |
| **Implement(구현)** | 제안을 **생산 코드로 실현**한다(파일 매니페스트). 테스트 코드는 안 건드림 | "제안을 행위 보존하며 코드로 옮기는 법"을 아는 지점. 진단은 안 함 |
| **Validate** | worktree 적용+테스트로 **행위 보존을 사실 확인**하고 검증불가 주장을 거른다 | "이 코드가 진짜 안전·검증가능한 변경인가"를 아는 유일 지점. 품질 리뷰의 전제 |
| **Critique** | (GREEN 후) 제안 + **구현된 코드**를 보고 과설계·빠진 위반·최소성 훼손을 지적한다 | "변경의 적정량"을 아는 시니어 리뷰어. 점수는 안 냄. Eval과 병렬 |
| **Eval** | (GREEN 후) 고정된 루브릭으로 축별 **결정적 채점**을 한다(제안 + 코드 근거) | 루브릭(잣대)을 소유. Critique와 병렬(서로 못 봄) |
| **Orchestrator(runner)** | 루프·lineage·롤백(worktree 폐기)·병렬 리뷰·수렴 종료를 조율한다 | 생성/조립 책임. **맨 마지막**에 정의(RDD ④) |

- private 메서드 분할이 아니라 **객체 경계**로 갈랐다. **진단(판단)≠구현(변환)** 을 다른 객체로 분리하되,
  Critique/Eval은 **구현된 코드까지** 보고 판단한다(변경 최소성·테스트 용이성은 실제 diff라야 채점됨).
- 생성(Diagnose/Implement)≠평가(Eval) 분리 유지는 B-6가 명시한 요구이자 generate-test 골격의 핵심이다.

---

## 4. 역할/포트 (RDD ③ — 갈아끼울 축이 있는 것만 추상화)

| 포트(역할) | 왜 정당한가(변경 축) | 지금 구현 |
|---|---|---|
| `LLMClient` | 공급자 축(codex↔claude)이 **실제로 둘 있다** → DIP/전략 정당 | 재사용(기존) |
| `BehaviorGate` (적용+테스트 실행) | 빌드툴 축(mvn→gradle) — **아직 mvn/JDK17 하나뿐** | **구체(mvn)로 두되 함수 하나로 격리.** 포트화는 보류 |

> **과설계 표시(YAGNI):** `BehaviorGate`를 지금 인터페이스로 뽑는 건 과설계다 — gradle 대상이
> 생기기 전엔 축이 없다. "언젠가 다른 빌드툴"은 정당화가 아니다. 한 함수
> `run_safety_net(worktree, test_cmd) -> GREEN|RED|COMPILE_FAIL`로 **격리만** 해 두고, 축이
> 실제로 늘 때 포트로 승격한다. ISP(너비)·DIP(방향)는 지금 `LLMClient` 하나로 충분.

---

## 5. 단계 상세 (Diagnose · Implement · Validate · Critique∥Eval)

> 순서: Diagnose(1) → Implement(2) → **Validate(3, 행위 게이트)** → **Critique ∥ Eval(4, 병렬 품질 리뷰)**.
> 행위 게이트를 먼저 통과한 코드만 품질 리뷰한다. Critique/Eval은 제안 + 구현된 코드를 보고 판단하며
> 서로 못 보므로 병렬 실행한다.

### Step 1 · Diagnose(진단) — `diagnose_refactor.md`
- **입력:** 대상 코드 + `change_goal` + `boundary` + 공유 참조(`smell-solid-map`). 1-1 테스트 케이스는 안 봄.
- **출력(설계만, 코드 없음):**
  ```
  {
    "violations": [{ "smell":"#9 Feature Envy", "principle":"SRP",
                     "where":"파일:심볼", "why":"메커니즘 1문장", "gate":"GO|DEFER|LEAVE|REMOVE" }],
    "proposals":  [{ "id":"R1", "technique":"파울러 기법명", "type":"A|B|C", "v":null,
                     "targets":["파일"], "addresses":"위반ref", "rationale":"..." }]
  }
  ```
- 참조 1부(탐지)→2부(게이트 A/B/C·v)→3부(SOLID)를 절차로 태운다. Rich Domain 전환을 제안에 포함.
- **`files`를 내지 않는다** — 코드 실현은 Step 2.

### Step 2 · Implement(구현) — `implement_refactor.md`
- **입력:** 원본 **생산** 코드 + Diagnose 제안(`gate="GO"`/REMOVE만) + `boundary`.
- **책임:** 제안을 **충실히 생산 코드로 실현**한다. 진단·새 제안 금지. 행위 보존(예외 종류·순서·부수효과) 원본과 동일.
- **테스트 코드는 안 건드린다.** 제안이 경계 suite 컴파일을 깨면 그건 suite의 경계-클린 결함(전제 조건)이지
  글루를 고칠 일이 아니다 — 신호로 남긴다(§3 ②).
- **출력:** `files` 매니페스트(생산 코드 전체). 이게 Validate의 적용 대상이자 Critique/Eval의 리뷰 대상.

### Step 3 · Validate — 금지패턴(텍스트) + 행위 게이트(사실)

품질 리뷰의 **전제**. GREEN이어야 Step 4로 간다. RED면 Implement refine.

1. **금지패턴 필터**(`check_forbidden_assertions` 재사용, 패턴만 교체):
   검증불가 리팩토링 주장 차단 — `"더 깨끗", "더 읽기 좋", "cleaner", "should be"`(수치 없이).
   위반 = `contract_error` → refine implement.

2. **행위 게이트 — 어떻게 GREEN을 커버하나** (규칙):
   - **① 동결되는 건 `.feature`(행위 명세)뿐 — 이것이 심판.** 유스케이스 진입점(`reserveTicket`)을
     도메인 언어로 관통하며 타입 이름을 안 쓴다. 개명·이동이 절대 못 건드림 → 부패 불가한 판정자.
   - **② 글루도 경계-클린이어야 하고, 파이프라인은 테스트 코드를 안 건드린다.** 셋업·단언을 안정 경계
     (진입점 `reserveTicket`)로만 한다 — 내부 접근자(`setReserved` 등)에 손대는 글루는 **결함**이다.
     제안이 그런 글루의 컴파일을 깨면 그건 "suite가 경계-클린이 아니다"라는 **신호**이지, 파이프라인이 글루를
     고칠 일이 아니다. suite 위생은 **리팩토링 루프 밖 전제 조건**(한 번 정리). → 심판(테스트 코드)은 파이프라인이
     절대 안 건드리므로 부패 불가.
   - **③ 리팩토링 전 원본에서 `.feature` GREEN을 기준선으로 잠근다.**
   - **④ worktree에 매니페스트(생산 코드) 적용 후 `.feature`만 실행.** 전부 GREEN → 행위 보존 PASS.
     ```
     git worktree add <tmp> <baseline>      # 원본은 안 건드림
     매니페스트 적용 → mvn test -Dtest=*AcceptanceTest
     GREEN → PASS  /  RED → 폐기 + contract_error "behavior_broken:<시나리오>"
     ```
   - **⑤ `.feature`(행위 명세)가 바뀌어야 통과한다면 = 리팩토링 아님.** 관측 행위/진입점이 바뀐 API 변경이므로
     반려·에스컬레이트한다. (내부 타입 개명은 여기 해당 없음 — `.feature`는 그대로다.)
   - **⑥ 구조 결합·순서의존 테스트는 게이트에서 제외.** 커버 못 하는 행위(coverage gap)만 신호로 남긴다.

> 언어 확장: 게이트 실행은 `BoundaryOracle(project, spec) → GREEN|RED|...` 포트로 격리하고
> 지금은 java-cucumber 어댑터 하나만 둔다. 다른 언어는 어댑터 추가로(포트는 §4, YAGNI).

### Step 4 · Critique ∥ Eval (병렬 품질 리뷰 — GREEN 후)

Validate GREEN을 통과한 코드만 리뷰한다. 둘은 **서로 못 보므로 병렬 실행**(runner의 ThreadPoolExecutor).
둘 다 **제안 + 구현된 코드**를 본다.

**Critique** — `critique_refactor.md`. "시니어 리뷰어" 역할. **점수·판정 금지.**
지적 축: ①과설계(참조 2부 게이트 Type B·v<2로 결정적 판정) ②빠진 위반 ③행위 바꿀 위험 ④변경 최소성 훼손(diff로).
출력 = `weaknesses:[{severity, axis, where, suggestion}]`.

**Eval** — `eval_refactor.md` + `rubrics/refactor.rubric.json` (4축 사다리). 각 축은 "구체 조건이 모두 충족된 가장 높은 칸".
**LLM은 축 점수만** 내고, `weighted_total`·threshold는 **runner가 결정적으로** 계산(`score_eval`).

| 축 | weight | 사다리 요지(1→5) |
|---|---|---|
| `diagnosis_accuracy` | 0.30 | 1 오진/원칙명 없음 → 3 원칙+왜, 일부 누락 → 5 누락0 + 구체 심볼 앵커 + 변경축 연결 |
| `change_minimality` | 0.25 | 1 무관 대규모 재작성 → 3 한두 곳 과함 → 5 위반1↔파울러기법1 추적가능(B-5 커밋 규율) |
| `behavior_preservation_risk` | 0.25 | 1 공개계약·예외타입 변경 근거없음 → 3 위험 인지·완화없음 → 5 순수 구조이동만+불변 논증 |
| `testability_improvement` | 0.20 | 1 언급없음/악화 → 3 어떤 테스트가 쉬워지나 지목 → 5 before(Mock N)→after(K) 수치 + 새요구 격리 |

- `thresholds`: `min_total: 4.0`, `min_axis`: 각 3.5, **`behavior_preservation_risk: 4.0`**(안전 최우선).
- **이중 방어:** Eval의 `behavior_preservation_risk`는 **정적 판단**, Validate(Step3)는 테스트로 **사실 증명**.
  행위는 이미 GREEN이 보장하고, Eval은 "설계·구현 품질"을 본다. Eval 미달 → refine(진단/구현 귀속).
- **실측:** 행위 GREEN이어도 Eval passed=False 가능(예: `testability_improvement=3<3.5`) — "행위 보존 ≠ 품질 충분".

---

## 6. 왜 새 mode가 아니라 형제 스킬인가 + 실패 처리 (설계 결정)

- generate-test 불변식: **critique/eval/refine·validate 골격은 mode 무관 공유**, gen+rubric만 갈림.
  B-6의 critique(과설계 지적)·eval(코드품질 4축)·validate(worktree)는 **도메인 자체가 다르다.**
  이를 mode로 밀어넣으면 공유 스테이지가 도메인 분기로 오염된다 → 형제 스킬이 옳다.
- **실패 라우팅:**
  - **Eval 미달** → Eval이 제안+코드를 다 보므로 **원인을 귀속**: 진단이 틀렸으면 `refine diagnose`,
    구현이 제안에서 어긋났으면 `refine implement`.
  - **Validate RED = 행위 깨짐** → `refine implement`(재구현). worktree 폐기로 롤백 공짜.
    구현이 제안에 충실한데도 RED면 → 설계로 에스컬레이트(제안 자체가 행위를 바꾼 것).
  - 모든 경로가 generate-test의 `contract_error→refine→max_iter` 배관과 **1:1 재사용**.
  - (선택) Diagnose 직후 **싼 결정적 가드**(LLM 아님)로 명백 불량 제안(경계 변경·B/v<2를 GO로)만 쳐내
    Implement 낭비를 줄일 수 있다. v0엔 미포함 — 로그가 낭비를 실증하면 추가(YAGNI).
- **fix-forward fast-path(F2 기계적 슬립 1회 수정)는 v0에서 만들지 않는다** — 과설계.
  `runs/` 로그가 F2 실패의 빈발을 실증할 때만 붙인다(OCP: 축이 실제로 늘 때만).

---

## 7. 정보 차단 (순환성 끊기 — generate-test §4 재사용)

- Diagnose는 eval/critique를 못 본다.
- Implement는 **Diagnose 제안 + 원본 코드 + 참조**만 받는다(구현에 필요한 것만). Critique/Eval보다 앞이라 자연히 못 본다.
- Critique는 eval 점수·validator 판정을 못 본다(제안 + 코드는 본다).
- Eval은 critique를 못 본다(제안 + 코드는 본다).
- Refine은 eval 원문 총점을 못 본다 — `weak_axes` + `contract_errors`(RED 시나리오 포함)만 받는다.

---

## 8. runs/ 로그 & 산출물 매핑 (수행내용 #2·#3)

- `runs/refactor/<날짜>_<hash>/`에 iter별 `gen/critique/eval` JSON + `final|failed` 보존(재사용 구조).
- **과설계 사례 기록(수행내용 #3):** Critique가 `over_engineering` severity로 지적한 항목과
  Eval `change_minimality`/`behavior_preservation_risk` 미달 건을 `runs/`에서 뽑아
  `docs/overengineering-notes.md`로 정리.
- **B-2/B-5 비교(수행내용 #2):** 대상 = B-2의 죽은 `TicketService`. 파이프라인 제안 vs
  `task2/task5-history`의 손 리팩토링(C0→C6)을 축별로 나란히:
  진단이 같은 위반을 짚었나 / 변경 최소성 / 파울러 기법 대응 / 테스트 GREEN 유지 여부.
- **slow-loop은 v0 범위 밖.** 쌓인 run 로그로 *파이프라인 자체*(프롬프트·루브릭)를 고치는
  자가개선 층은 넣지 않는다(run이 0개라 입력 없는 기계 = 과설계). 이 §8 로그 구조를
  generate-test와 호환되게 남겨 **나중 Phase 2에서 `analyze_runs`+`proposer` 골격을 붙일 훅**으로 둔다.

---

## 9. 구현 상태 & 미해결 (BDUF 경계 — 구현이 검증)

**구현됨(실측 관통, `runs/c0/`):**
- Diagnose → Implement → Validate(행위 게이트) → Critique ∥ Eval 병렬. `runner.py`가 오케스트레이션.
- 결정적 게이트(`behavior_gate.py`): worktree 적용 + mvn + GREEN/RED/COMPILE_FAIL. 테스트 코드 안 건드림.
- Validate RED → Implement refine 루프. Eval 축 점수 → `score_eval`이 weighted_total·threshold 결정적 판정.
- baseline = tag `refactor-agent-c0-baseline`(원본 절차 코드 + 경계-클린 글루).
- 실측: 행위 GREEN, Eval passed=False(testability 3<3.5), Critique 약점 6건.

**아직 안 됨:**
1. **Eval 미달 → refine diagnose/implement 루프** — 현재는 판정·보고만(설계 루프 미배선).
2. **금지패턴 텍스트 필터** — 설계엔 있으나 runner 미배선(행위 게이트만).
3. Critique/Eval → Critique 아직 refine 신호로 안 흘림.
4. 공유 코어 추출·slow-loop·다언어 어댑터는 보류(YAGNI).

**미해결 가설:** `behavior_preservation_risk` 사다리가 Validate 실측과 얼마나 일치하나 → 로그로 캘리브레이션.

> 이 문서는 확정 계약이 아니다. 그림의 오류는 구현·테스트가 잡는다(위 "실측"이 그 예).
