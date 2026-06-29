# Slow Loop — 자기 개선 루프 설계

## 배경

이 설계는 외부 피드백 네 가지 질문에서 출발한다.

1. 검사 장치는 "무겁게 검사하기로 한 답변"에만 작동한다. 정작 위험한 답은 평범한 얼굴로 온다. 무엇을 보고 "무겁게 검사해야 한다"를 정하나? 사람이 정하면 컨디션 문제로 되돌아온다.
2. 검사를 100번 했다고 101번째가 더 똑똑한가? 지금 구조에서 검사 기준은 누가, 언제, 무엇을 보고 고치나?
3. 검사할 때마다 "어떤 항목이 약했다"는 기록이 남는다. 그 기록은 지금 어디로 가나? 쌓이고 끝인가, 다음 검사를 바꾸나?
4. 기록을 모아 검사 기준 자체를 고친다면, 그건 답변 하나를 검사하는 루프와 다른 더 느린 두 번째 루프다. 이 루프가 생기면 장치는 "검사기"에서 무엇으로 바뀌나?

기존 파이프라인(fast loop)은 초안 하나를 생성·비평·평가·판정한다. 이 문서가 정의하는 slow loop은 그 fast loop이 남긴 run 기록을 모아, **파이프라인 자체를 고치는 제안을 만든다.** 4번 질문의 답: 검사기 → 자기 자신을 개선하는 시스템.

## 핵심 원칙

### 1. fast loop의 분리 원칙을 한 겹 더 적용한다

fast loop은 "점수 매기기(LLM, 흔들림)"와 "기준 넘었는지 판정(코드, 안정)"을 분리했다. slow loop도 같은 방식으로 3분할한다.

| 단계 | 주체 | 흔들림 | 산출 |
| --- | --- | --- | --- |
| 신호 집계 | 코드 (`analyze_runs.py`) | 없음 (deterministic) | `analysis.json` |
| 수정 제안 | LLM (proposer stage) | 있음 | `proposals/YYYY-MM-DD.md` |
| 기준 변경 | **사람** | — | 대상 파일 + `CHANGELOG.md` |

최종 기준 변경은 사람이 한다. 피드백 2번("누가 기준을 고치나")의 답이며, 전역 AGENTS.md의 "사용자 확인 없이 규칙 파일을 직접 수정하지 않는다" 원칙과 일치한다.

### 2. 낮은 점수는 "어디를 고칠지" 말해주지 않는다

`evidence = 3.0`의 원인은 최소 다섯 곳이다.

| 가능한 원인 | 고쳐야 할 대상 |
| --- | --- |
| 생성이 근거를 약하게 씀 | `prompts/gen_system.md` |
| 퇴고가 근거를 못 살림 | `prompts/refine_system.md` |
| 기준(bar)이 잘못 맞춰짐 | `rubric.yaml` |
| 평가가 근거 축을 과/소평가 | `prompts/eval_system.md` |
| intake가 근거 될 재료를 못 담음 | `AGENTS.md` / intake 규칙 |

따라서 `analyze_runs.py`는 target을 정하지 않는다. **신호와 후보만** 띄운다. 어느 것이 진짜 원인인지 진단하는 일은 proposer(LLM)가, 최종 결정은 사람이 한다. 피드백 2번의 답: 누적된 rationale이 증상이 아니라 근본 원인을 짚게 해줄 때만 시스템이 똑똑해진다.

### 3. slow loop은 모든 것을 고칠 수 있다

수정 대상은 rubric에 한정되지 않는다. 파이프라인 코드, 각 stage system prompt, 전역 AGENTS.md를 모두 포함한다.

## 트리거

`run_draft.py`가 draft를 완료한 뒤 `runs/pending/` 디렉토리 개수를 센다. **5개 이상**이면 slow loop을 실행한다. 별도 cron 없이 이벤트 기반으로만 동작한다. 데이터가 있을 때만 발동한다는 뜻이다.

## 신호 출처 → target 후보 매핑

신호는 `eval.json` 점수에 한정되지 않는다.

| 신호 | 시사하는 target 후보 |
| --- | --- |
| 같은 axis 반복 미달 | `rubric.yaml` 또는 gen/refine prompt |
| critique가 같은 지적 반복 | gen/refine prompt (생성이 못 잡음) |
| eval rationale에 "이건 못 봤다" 반복 | `eval_system.md` (평가 시야) |

`analyze_runs.py`는 통과한 run의 `eval.json`, `critique.json`을 긁는다. ERROR와 `failed.json`은 제외한다(위 "대상" 참조). validation 반복 실패와 failed run 패턴은 v3에서 다룬다.

패턴 임계값(기본): **한 axis가 pending의 60% 이상에서 `min_axis` 미달이면 제안 후보로 올린다.** (pending 5개 중 3개)

## 단계별 설계

### analyze_runs.py (코드, deterministic)

- 입력: `runs/pending/` 전체
- 출력: `analysis.json`
- 대상: **통과한 run의 eval/critique만** 수집한다. ERROR(파이프라인 크래시)는 인프라 노이즈라 제외하고, `failed.json`(max_iterations 미통과)도 표본을 왜곡하므로 v2에서는 제외한다. failed run의 "시스템이 못 고친 케이스"라는 신호는 가치가 있으나 v3에서 별도로 다룬다.
- 책임:
  - axis별 평균/최저 점수
  - axis별 "기준 미달 run 비율"
  - 미달 axis의 rationale 텍스트 모음
  - critique 반복 지적 집계
  - 패턴 룰 적용 → 제안 후보 목록
- 금지: rubric/prompt/코드 직접 수정, 품질의 주관적 판단, target 단정, ERROR/failed run 수집.

### 제안 파이프라인 (LLM, fast loop과 같은 gen/critique/eval/refine 구조)

slow loop도 fast loop과 같은 4단계 오케스트레이션을 쓴다. 단, **평가 대상이 글이 아니라 시스템 수정안(diff)이므로 rubric의 축과 prompt 내용을 수정한다** (비판 1).

재사용하는 범위를 넓게 잡는다. AI가 백지에서 다시 쓰면 검증된 형식과 로더 코드까지 갈아엎을 위험이 있어 낭비·불안정하다. 따라서:

- **재사용**: 오케스트레이션(파일 핸드오프, 검증 게이트, 정보 차단) + rubric 스키마 포맷(scale 0–5, weights, thresholds 구조)과 그것을 읽는 `validate.py` 로더.
- **수정/교체**: rubric의 축과 내용, 그리고 prompt의 내용.

즉 `writing:v1`의 점수 축을 그대로 쓰지는 않지만, 그 포맷과 코드는 재사용한다. 새로 만드는 것은 `proposal:v1`의 **축**과 `propose_*_system.md`의 **내용**이다.

- **gen**: `analysis.json` + 현재 버전의 후보 target 파일들(rubric, 모든 prompt, AGENTS.md, 관련 stage 코드)을 읽고, 근본 원인을 진단해 대상별 **구체 diff 초안**을 만든다. 고위험 target(pipeline 코드, AGENTS.md)도 diff 초안까지 작성한다.
- **critique**: 제안의 약점을 짚는다. 근거가 신호에 실제로 닿는가, 증상이 아니라 원인을 짚었는가, diff가 적용 가능한가.
- **eval**: 제안 전용 rubric `proposal:v1`로 점수와 축별 근거를 낸다. critique에 anchor되지 않는다.
- **refine**: critique·검증 신호를 반영해 제안을 다듬는다. 단 **"구체화·축소" 방향으로만** 허용한다. "더 확신에 찬 같은 오답"을 내놓지 않게, 진단 자체를 키우는 재작성은 금지.

제안의 검증은 fast loop과 같은 원칙으로 **기계적 게이트(validator)** 와 **판단 채점(rubric)** 으로 나눈다. 상세는 아래 "## 제안 검증" 참조.

- 입력 경계: 제안 파이프라인은 `analysis.json` + 후보 target 파일들을 읽는다.
- 출력: gate 통과 시 `changelog/proposals/YYYY-MM-DD.md` (= proposal-final). 통과 못 하면 fast loop과 동일하게 `failed` 산출을 남긴다.
- 금지: 파일 직접 수정, 폴더 이동 판단, 사람 승인 없는 적용.
- provider: `run_draft.py`가 사용한 provider를 그대로 상속한다.

**자기 자신은 자동 개선 대상이 아니다 (비판 2의 고정점 원칙).** slow loop이 쓰는 rubric(`proposal:v1`)과 prompt는 사람만 바꾼다. slow loop이 자기 기준을 자동으로 고치면 순환이 끊기지 않는다. 그 변경이 정말 나은지 검증하는 메타 장치(적중률 측정, 케이스 샘플 비교)는 v3로 미룬다 (아래 "단계" 참조).

### proposal 문서 형식

```markdown
# Slow Loop Proposal — 2026-07-05 (pending 6개 분석)

## 신호 요약
- evidence: 6개 중 4개 min_axis 미달, rationale 공통 "결정 장면의 질감 부족"
- critique: "왜 그때 결정했는가"가 3개 run에서 반복 지적

## 제안 1 — [위험: 낮음] prompts/gen_system.md
근거 신호: evidence 미달 + critique 반복
진단: 기준 문제가 아니라 생성이 '결정 장면'을 안 쓰는 것으로 추정
diff:
  + "각 설계 결정에 그 결정을 내린 구체적 장면(언제/무엇을 보고)을 최소 1개 포함하라"

## 제안 2 — [위험: 중] rubric.yaml
...

## 제안 3 — [위험: 높음] pipeline 코드 / AGENTS.md
diff 초안 + "사람 검토 필수" 표시
```

위험도 티어로 사람이 검토 집중도를 조절한다. 코드와 AGENTS.md 변경은 항상 "높음".

## 제안 검증

fast loop이 기계적 판정(`validate.py`)과 LLM 채점(rubric)을 나눈 것처럼, 제안도 둘로 나눈다. 결정적으로 판정 가능한 것은 validator(코드)로 빼고, rubric에는 판단이 남는 축만 둔다.

### validator (코드, PASS/REJECT, 100% 결정적)

채점이 아니라 통과/탈락. LLM을 거치지 않는다. 하나라도 실패하면 그 제안은 REJECT.

| 검사 | 기계적 판정 |
| --- | --- |
| 신호 실재성 | 제안이 인용한 신호가 `analysis.json`에 실제로 있는가 |
| diff 적용성 | diff의 앵커 텍스트가 대상 파일에 존재하는가 |
| 위험 라벨 규칙 일치 | 코드/AGENTS.md를 건드리면 위험="높음"으로 표기됐는가 |
| 범위 임계 | 한 제안이 건드리는 파일/hunk 수 ≤ 임계(기본 파일 1, hunk 3) |

### rubric `proposal:v1` (LLM, 판단, 5축)

각 축은 **사다리(ladder)** 다. 점수 = "아래 조건이 모두 충족된 가장 높은 칸". "얼마나 좋은가"가 아니라 "이 구체 조건이 있는가"만 판단하므로 재현성이 높다. scale 0–5, `writing:v1`의 포맷·로더를 재사용한다.

```yaml
1. 진단 구체성        weight 0.30
   0: target 파일 미지정 (증상에서 멈춤)
   2: target 파일 1개 이상 지목
   3: + 어느 단계/규칙이 원인인지 명시
   4: + 왜 그 단계가 원인인지 메커니즘 문장 1개 이상
   5: + 그 메커니즘이 analysis.json의 구체 신호와 연결

2. 효과 개연성        weight 0.25
   0: 겨냥한 axis를 명시 안 함
   2: 어떤 axis를 올리려는지 명시
   3: + diff가 그 axis에 영향 준다는 연결 진술
   4: + 변경 → 글 산출 변화 → axis 개선의 경로를 단계로 서술
   5: + 그 경로를 뒷받침하는 신호/사례 인용

3. 대안 배제          weight 0.20
   0: 다른 원인 가능성 언급 없음
   2: 경쟁 원인 1개 이상 언급
   3: + 왜 그게 아닌지 진술
   5: + 배제 근거를 analysis.json 신호로 제시

4. 부작용 인식        weight 0.15
   0: 부작용/트레이드오프 언급 없음
   2: 부작용 가능성 일반 언급
   3: + 어느 axis나 단계에 부작용 가능한지 지목
   5: + 그 부작용을 완화/감시할 방법 제시

5. 우선순위 타당성    weight 0.10
   0: 여러 제안의 순서/근거 없음
   2: 순서는 있으나 근거 없음
   3: + 신호 강도 또는 위험을 근거로 순서 제시
   5: + 신호 강도 대비 위험을 함께 고려해 정렬

thresholds:
  min_total: 3.8
  min_axis: { 진단 구체성: 3.0, 효과 개연성: 3.0, 그 외: 2.5 }
```

특수 규칙:

- **제안이 1개면** "우선순위 타당성"은 정렬할 대상이 없으므로 **3.0(중립)으로 고정 채점**한다. 축을 제외·재정규화하지 않는 이유: `validate.py`가 eval 점수의 축 집합과 rubric 축 집합의 정확한 일치를 요구한다(`set(scores.keys()) != axes`). 고정 중립값을 쓰면 로더를 그대로 재사용할 수 있다. 가중치 0.10이라 영향이 작고 threshold 2.5는 통과한다.
- 점수는 제안 **하나 단위**로 매긴다. 한 문서에 제안이 여럿이면 각 제안이 따로 채점·게이트되고, validator에서 떨어진 제안은 rubric에 가지 않는다.

## 상태 표현: 폴더 위치

검토 여부를 별도 문서에 기록하지 않는다. 별도 문서를 두면 폴더와 문서가 둘 다 진실의 출처가 되어 동기화 실패 지점이 생긴다(피드백 1번이 지적한 문제). 상태는 위치로만 표현한다.

```
runs/
  pending/                  # run_draft.py가 여기 씀 (미검토)
    2026-06-29_3930539b/
  reviewed/                 # slow loop이 분석을 끝내면 여기로 이동
    2026-06-29_3930539b/
```

- 트리거 조건 = `pending/` 디렉토리 개수
- proposal 생성에 성공한 직후 pending → reviewed로 이동한다. 이동이 "이 run들은 분석에 반영됐다"는 증거다.
- `eval.json`의 `source_files`는 파일명만 쓰므로 폴더 이동으로 깨지지 않는다.

## 버저닝

artifact에 이미 component별 버전 스탬프가 있다. `eval.json`의 `rubric_name: writing:v1`, metadata의 `prompt_version: eval_system:v1`. 따라서 각 run은 자기가 어떤 버전들로 돌았는지 self-describing이다.

- 각 component는 자기 버전 태그를 유지한다 (`rubric:vN`, `gen_system:vN`, ...).
- 변경이 승인되면 **단일 `CHANGELOG.md`**에 한 줄을 남긴다: 날짜 / 무엇을 / 왜 / 근거 run hash / 위험도.
- 과거 버전 스냅샷은 따로 저장하지 않는다. git이 이미 모든 변경 이력을 갖고 있으므로 중복이다. 재현이 필요하면 CHANGELOG의 git 커밋 해시로 되돌아간다.

CHANGELOG 예시:

```markdown
## gen_system:v2 (2026-07-05)
- 변경: 각 설계 결정에 구체적 장면 1개 이상 포함 지시 추가
- 근거: pending 6개 중 4개 evidence 미달, critique "결정 장면 부족" 반복
- 분석 run: 3930539b, 4ef5692d, ...
- 위험: 낮음
- commit: <적용 커밋 해시>
```

## 정보 차단 규칙: slow loop 예외

fast loop은 stage 간 정보를 엄격히 차단한다(eval이 critique에 anchor되지 않게). slow loop에는 두 층위의 규칙이 함께 적용된다.

- **층위 1 (제안 파이프라인 내부):** fast loop과 동일하게 차단한다. 제안 critique가 제안 eval에 anchor되지 않는다.
- **층위 2 (context 입력):** 제안의 산출물(diff)은 관계적이라 대상 파일을 봐야 판단이 된다. 따라서 각 단계는 context(대상 파일)는 읽되, **다른 단계의 산출물은 읽지 않는다.**

단계별 접근:

| 단계 | 읽는 것 | 안 읽는 것 |
| --- | --- | --- |
| gen | `analysis.json` + 후보 target 전체 (rubric, 모든 prompt, AGENTS.md, 관련 코드) | 다른 단계 산출물 |
| critique | `analysis.json` + 제안 + 제안이 건드린 파일만 | 제안 eval, 다른 단계 산출물 |
| eval | `analysis.json` + 제안 + 제안이 건드린 파일만 + `proposal:v1` rubric | 제안 critique |
| refine | `analysis.json` + 직전 제안 + critique + 제안이 건드린 파일만 | 제안 eval 총점 원문 |

gen만 후보 전체를 넓게 읽고(target을 골라야 하므로), 나머지는 제안이 가리키는 파일만 좁게 읽는다.

**이중 방어로 강제한다.**

- **1차 (강제):** runner(`proposer.py`)가 각 단계에 위 표의 허용 파일만 payload로 넣는다. 받지 못한 산출물은 읽고 싶어도 못 읽는다. 기존 `pipeline/AGENTS.md`의 "runner가 payload 구성을 제어한다" 원칙을 따른다.
- **2차 (지시):** 각 `propose_*_system.md`에 "받은 context와 제안만 보고 판단하라, 다른 단계 산출물을 찾지 말라"를 명시한다.

이 두 층위가 fast loop과 다른 점이라는 것을 `pipeline/AGENTS.md`에 명시한다.

## 신규 / 변경 파일

신규:

```
pipeline/
  changelog/
    CHANGELOG.md
    proposals/YYYY-MM-DD.md
  stages/proposer.py            # 제안 gen/critique/eval/refine 오케스트레이션
  prompts/
    propose_gen_system.md
    propose_critique_system.md
    propose_eval_system.md
    propose_refine_system.md
  rubric_proposal.yaml          # proposal:v1 (글 rubric과 별개)
  schemas/analysis.schema.json
  analyze_runs.py               # 신호 집계 + 제안 파이프라인 호출 + 폴더 이동
runs/
  pending/
  reviewed/
```

변경 (최소):

- `run_draft.py`: 출력 경로 `runs/` → `runs/pending/`, 완료 후 트리거 체크 블록 추가
- `pipeline/AGENTS.md`: `## Slow Loop` 섹션 + 제안 단계 역할 경계 + 정보 차단 두 층위 명시
- 새 CLAUDE.md는 만들지 않는다. nested AGENTS.md가 자동 로드되며, 관리 포인트를 늘리지 않기 위해 기존 파일에 섹션을 추가한다.

## 단계 (v1 / v2 / v3)

### v1 — 이미 있음: fast loop 본체

초안 하나를 gen/critique/eval/refine으로 생성·판정하는 기존 파이프라인.

### v2 — 지금 만든다: slow loop 본체

- `analyze_runs.py` (신호 집계, deterministic)
- 제안 파이프라인 (gen/critique/eval/refine + `proposal:v1` rubric)
- `proposal-final` 산출 → 사람에게 제안
- pending/reviewed 폴더 상태, 트리거(pending ≥ 5)
- 버저닝 + 단일 CHANGELOG
- **고정점 원칙은 규칙으로 명시**: slow loop 자신의 rubric/prompt는 사람만 변경 (기계 없이 문장으로)

### v3 — 미룬다: 검사기를 검사하는 메타 장치

지금 만들어도 검증할 데이터가 없어서 미룬다.

- **적중률 측정 (탐지)**: 적용된 제안이 겨냥한 axis가 실제로 개선됐는지 후속 run으로 측정. proposer가 체계적으로 오진하는지 자동 신호.
- **케이스 샘플 비교 (검증)**: slow loop 자신의 rubric/prompt를 바꿀 때, 구버전 vs 신버전을 최근 사례에 돌려 사람이 라이브 비교. 정답 키가 아니라 회전하는 사례 + 라이브 판단.
- **failed/ERROR 신호 분리**: `failed.json`(시스템이 못 고친 케이스)과 validation 반복 실패를 통과 run 분석과 별도 트랙으로 다룬다.

이 둘이 들어와야 "그냥 제안 생성기"에서 진짜 자기 학습으로 넘어간다. v2는 그 전 단계다.

## 확정 결정 요약

- 자동화 범위: **제안만**. rubric/prompt/코드/AGENTS.md 어느 것도 자동 수정하지 않는다.
- 제안 생성: fast loop과 같은 gen/critique/eval/refine 구조, 단 `proposal:v1` rubric으로 (글 rubric 재사용 안 함).
- 트리거: `run_draft.py`가 pending ≥ 5에서 발동 (이벤트 기반).
- 제안 권한: 고위험 target 포함 **구체 diff 초안까지** 작성. 적용은 사람.
- provider: `run_draft.py` provider 상속.
- 상태 표현: pending/reviewed 폴더 이동.
- 버저닝: component별 버전 태그 + 단일 CHANGELOG (과거 버전은 git이 관리, 스냅샷 없음).
- 고정점: slow loop 자신의 rubric/prompt는 사람만 변경. 메타 검증은 v3.

## 열린 질문

- 제안이 한 번에 다루는 target 개수 상한이 필요한가? (검토 부담 관리)
- reviewed/가 무한히 쌓일 때의 보관/삭제 정책.
