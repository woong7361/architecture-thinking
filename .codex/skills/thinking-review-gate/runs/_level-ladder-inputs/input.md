# User Input

thinking-review-gate Level 3로 이 설계 판단을 검토해줘.

질문:
thinking-review-gate에서 Level 1, Level 2, Level 3를 나눈 설계가 적절한가?

초안:
"""
Level 1은 빠른 inline self-review, Level 2는 critique 기반 inline review, Level 3는 file hand-off agentic review로 둔다.
이렇게 나누는 이유는 답변 속도와 품질 사이의 trade-off를 조절하기 위해서다.
모든 답변을 Level 3로 보내면 생산성이 떨어지고, 아무 검토도 하지 않으면 근거 없는 단정 때문에 재작업이 늘 수 있다.
따라서 모든 답변은 최소 Level 1을 거치고, 설계/추천/구현 방향은 Level 2, artifact와 gate가 필요한 검토는 Level 3로 올린다.
"""
