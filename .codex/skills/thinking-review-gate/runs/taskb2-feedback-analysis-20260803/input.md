# Original User Input

task2\assignments\taskB-2.md 의 피드백을 보고 각각의 피드백에 대해 

내가 주장한 내용, 피드백이 날아온 내용, 피드백에 대한 답변이나 변론 2-3가지, 거기에 대한 트레이드오프를 보여줘 

이때 단순한 이모지나 동의와 같은 피드백은 제외해줘 

이걸 스킬로 먼저 만들고 실행해줘


skill이 사용되는 순간은  task에 대한 피드백을 분석할때야 
그리고 대상은 task야 
반드시 기억해 skill은 특정 부분에 anchor가 아니라 공용으로 쓰는 것을


# Checked Context

# 검토 문맥

- 대상 task: `task2/assignments/taskB-2.md`
- 새 스킬: `.codex/skills/analyze-task-feedback/SKILL.md`
- UI 메타데이터: `.codex/skills/analyze-task-feedback/agents/openai.yaml`
- 대상 문서에는 피드백 4건이 있으며 모두 댓글이다. 본문 없는 리액션은 없다.
- 네 댓글은 칭찬 또는 동의 표현 뒤에 각각 새로운 질문, 범위 확장, 반례가 있으므로 모두 실질 피드백으로 분류한다.
- 피드백 ID와 위치는 FB-B2-01 L58, FB-B2-02 L66, FB-B2-03 L74, FB-B2-04 L84다.
- 원본 task와 피드백은 수정하지 않는다.
- 스킬은 특정 설계 주제나 사례의 명사를 규칙으로 사용하지 않고, 모든 task 피드백에 적용 가능한 선별·복원·대응·트레이드오프 절차로 작성했다.
- `skill-creator`의 공식 `quick_validate.py`는 실행했으나 로컬 Python에 `yaml` 모듈이 없어 시작 단계에서 중단됐다. 패키지는 임의 설치하지 않았다.
- 검토 기준: 원래 주장의 범위 보존, 질문을 반박으로 과장하지 않기, 후보 2~3개의 실질적 차이, 후보별 이득·비용·적합 조건, 원문에 없는 사실을 단정하지 않기.
