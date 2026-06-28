# 문제 기록

이 문서는 AI 작업 환경을 개선하는 과정에서 발견한 열린 문제를 기록한다.

문제 기록은 당장 해결하지 않아도 되는 설계 쟁점, 반복되는 실패 패턴, 추후 skill/rule/hook/agent 개선 후보를 잊지 않기 위한 것이다.

## 2026-06-28: thinking-review-gate의 verifier 출력과 hand-off 문제

상태: resolved

구현 기준: `.codex/skills/thinking-review-gate/`의 현재 구현을 최신 기준으로 본다.

통과 처리: 2026-06-28 사용자 결정에 따라 현재 구현과 문서 정합성 기준으로 통과 처리한다.

### 문제가 있는 파일

- `.codex/skills/thinking-review-gate/SKILL.md`
- `.codex/skills/thinking-review-gate/prompts/verifier.md`
- `.codex/skills/thinking-review-gate/rubric.yaml`
- `ai-answer-accuracy-gate.md`

### 문제

`thinking-review-gate` skill에서 verifier sub-agent를 사용할 때, 검토 결과를 어떤 방식으로 main agent에게 넘길지 고민하던 문제다.

현재 구현에서는 다음처럼 결정되었다.

- Level 2는 Markdown critique만 사용한다.
  - `prompts/verifier.md`는 점수, 가중 평균, pass/fail gate를 출력하지 않는다.
  - main agent는 문제 지점, 확인 필요, 수정 제안만 반영한다.
- Level 3에서만 file hand-off를 사용한다.
  - `critique.md`는 답변 수정을 위한 문제 지점과 수정 제안을 담는다.
  - `eval.json`은 축별 점수와 점수 이유만 담는다.
  - `validation.json`은 `validate.py`가 계산한 `weighted_score`, `weak_axes`, `gate_result`를 담는다.
- critique와 eval은 분리한다.
  - Level 3 runner는 critique agent와 eval agent를 병렬 실행한다.
  - eval agent는 `critique.md`를 읽지 않는다.
  - critique agent는 `eval.json` 또는 `validation.json`을 읽지 않는다.
- 점수는 기본 대화 경로에 노출하지 않는다.
  - Level 2 verifier output에는 점수를 포함하지 않는다.
  - Level 3 점수는 artifact와 gate 판단에만 사용한다.

### 현재 결정

- 일반 verifier output에는 축별 점수를 노출하지 않는다.
- Level 2 verifier는 점수를 계산하거나 출력하지 않는다.
- main agent에게는 문제 지점, 확인 필요, 수정 제안, 요약을 전달한다.
- 축별 점수와 점수 이유는 Level 3 `eval.json`에만 기록한다.
- 가중 평균, 약한 축, pass/fail gate는 Level 3 `validation.json`에만 기록한다.
- file hand-off는 Level 3 조건이 있을 때만 사용한다.

### 해결 조건

이 문제는 다음 조건을 모두 만족하면 완전히 해결된 것으로 본다.

- [x] 일반 verifier output에 축별 점수와 계산 과정이 노출되지 않는다.
- [x] Level 3 artifact에서만 축별 점수와 계산 과정을 볼 수 있다.
- [x] main agent가 일반 응답을 수정할 때 점수가 아니라 문제 지점과 수정 제안을 기준으로 반영한다.
- [x] critique와 eval의 역할이 문서 또는 prompt에서 구분되어 있다.
- [x] file hand-off를 사용할 조건과 저장 위치가 정해져 있다.
- [x] 실제 review case 2개 이상에서 verifier output을 확인했고, 점수 노출이 main agent 응답을 과도하게 anchor하지 않는다고 판단했다.

### 확인 방법

해결 여부를 확인할 때 다음을 점검한다.

- `prompts/verifier.md`의 기본 출력 형식에 축별 점수가 없는가?
- Level 3 점수 출력이 `eval.json`과 `validation.json`으로 분리되어 있는가?
- `SKILL.md`가 verifier를 호출할 때 점수를 최종 사용자 응답에 노출하지 말라고 지시하는가?
- review case 실행 결과에서 main agent가 점수 자체를 인용하지 않고 문제 지점만 반영하는가?
- file hand-off의 저장 위치, 파일명, 사용 조건이 `SKILL.md`와 scripts에 반영되어 있는가?

### 나중에 결정할 것

없음. 현재 구현 기준으로 통과 처리했다.
