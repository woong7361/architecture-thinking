# 사용자 개인화 — 설계

## 배경

기존 blog-draft는 개인 문맥을 전혀 쓰지 않았고(`problem.md` 참조), 느린 루프(slow loop)는 run의 eval/critique 신호만 먹을 뿐 **사용자가 최종 초안에 준 피드백**은 반영하지 못했다. 이 설계는 그 공백을 메운다: 초안이 시간이 지날수록 사용자에게 맞춰지도록, 피드백에서 교훈을 얻고 저자에 대한 전문화를 파일로 유지한다.

## 두 개의 에이전트, 두 개의 방향

여기엔 성격이 다른 두 층이 있다.

| 층 | 정체 | 입출력 | 피드백 |
| --- | --- | --- | --- |
| 파이프라인 stage | 백그라운드 `codex/claude -p` 서브프로세스 | JSON in → JSON out | 받지 못함 |
| 인터랙티브 스킬 세션 | blog-draft 스킬을 돌리는 Claude Code | 사용자와 대화 | 여기서만 캡처 |

그래서 개인화는 두 방향으로 갈린다.

- **아래로 (context → 파이프라인):** `soul.md` + `memory.md`를 gen/refine에 주입.
- **위로 (피드백 → 기억):** 스킬 세션이 `problem.md` / `memory.md` / `soul.md`에 기록.

## 파일 계층

세 파일을 한 파일로 합치지 않는 이유: 안정된 정체성, 누적되는 교훈, 원본 반응은 수명과 신뢰도가 다르기 때문이다.

| 파일 | 담는 것 | 성격 | 주입 |
| --- | --- | --- | --- |
| `soul.md` | 정체성·목소리·지속 취향 | 안정, 작게 유지 | gen / refine |
| `memory.md` | 피드백에서 얻은 교훈 | 누적 | gen / refine |
| `problem.md`의 "사용자 피드백 누적" | 세션 내 raw 반응 (pos/neg) | 로그 | 프롬프트에 미주입 |

`memory`는 원료(교훈 누적), `soul`은 정제된 정체성이다. 같은 교훈이 반복되면 사람이 확인 후 memory → soul로 승격한다.

## 주입 규칙: gen/refine만, eval은 깨끗하게

`stages/scripts/context.py`의 `load_persona_context()`가 `soul.md` + `memory.md`를 읽어 `AUTHOR_CONTEXT` 블록으로 gen/refine의 system prompt 앞에 붙인다. 파일이 없으면 빈 문자열이라 동작이 바뀌지 않는다.

**Critique / Eval에는 주입하지 않는다.** fast loop의 핵심 자산은 "생성(흔들림)과 판정(안정)의 분리"인데, eval에 개인 취향을 주입하면 판정자가 rubric이 아니라 취향에 anchor되어 "좋은 글"이 아니라 "취향에 맞춘 글"로 드리프트한다(Goodhart). 취향이 하드한 품질 기준이 되어야 한다면 느린 루프를 거쳐 `rubric.yaml`로 승격한다(사람 승인).

주입은 stage의 `build_prompt`에서 이뤄지므로 provider(codex / claude)와 무관하게 동일하다. 클라이언트별로 읽게 하지 않는 이유: 두 곳을 동기화해야 하고 stage별 선택 주입(eval 제외)이 불가능해지기 때문이다.

## 피드백 캡처

피드백은 파이프라인이 아니라 인터랙티브 스킬 세션이 캡처한다(`SKILL.md`의 workflow step 8). 다른 세션도 동작하도록 지시는 `SKILL.md`에 둔다 — 스킬 호출 시 항상 로드되기 때문이다.

부정/긍정 모두 신호지만 역할이 반대다.

| | 부정 | 긍정 |
| --- | --- | --- |
| 뜻 | "바꿔라" | "유지·보호해라" |
| 교훈 | 회피 교훈 | 성공 패턴 |
| 승격 | soul 회피취향 / rubric | soul 확정 목소리 |

긍정 피드백의 두 역할: (1) 정체성(soul)의 주 재료 — "어떤 글을 쓰는 사람인가"는 반복해서 잘 통한 것으로 정의된다. (2) 느린 루프의 회귀 방지 — proposal이 긍정 지점을 건드리면 경고 근거가 된다.

## 라우팅

- 부정/긍정 raw 반응 → `problem.md` "사용자 피드백 누적" (`verdict` 태그).
- 일반화 가능한 교훈 → `memory.md`.
- 명백한 지속 취향(특히 반복된 긍정) → 사용자 확인 후 `soul.md`. 확인 없이 규칙성 파일을 수정하지 않는다(전역 AGENTS.md 원칙).

## 느린 루프 연결

반복 피드백은 기존 느린 루프에 꽂힌다: proposer가 `problem.md` 피드백 섹션 + `soul/memory`를 context로 읽어, 같은 지적이 여러 번 나오면 proposal에 반영한다. `analyze_runs.py`의 파싱을 바꾸지 않고 context 주입만으로 처리한다.

## 단계

### v1 — 지금 만든 것

- `soul.md` / `memory.md` 신규, `problem.md`에 "사용자 피드백 누적" 섹션.
- `context.py` + gen/refine 주입 (eval/critique 제외).
- `SKILL.md`에 캡처 지시(step 8) + 개인화 섹션.
- 캡처(pos/neg) + 주입 + **수동 관리**까지.

### 이후

- memory → soul 자동 승격 제안 (반복 감지).
- proposer가 피드백 섹션을 context로 읽어 proposal에 반영 + 긍정 회귀 경고.
- draft metadata에 개인화 주입 여부 스탬프(재현성).

## 재현성 참고

`soul.md` / `memory.md`가 바뀌면 gen/refine의 실질 프롬프트가 달라진다. 어떤 run이 개인화를 받았는지 추적하려면 draft metadata에 플래그를 남기는 것이 이후 과제다. 현재는 git 이력으로 두 파일의 시점을 되짚는다.
