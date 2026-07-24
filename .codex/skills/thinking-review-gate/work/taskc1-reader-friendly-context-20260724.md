# 프로젝트 문맥

- 수정 대상 1: `task3/assignments/taskC-1.md`의 `답안 1: N-Tiered 한계 비평`.
- 수정 대상 2: 루트 `AGENTS.md`.
- 사용자는 제출물 1에 독자가 인과를 쉽게 이해할 수 있는 예시를 추가하라고 요청했다.
- 사용자는 모든 요약과 리포트를 보는 사람 관점에서 쉽게 읽도록 `AGENTS.md`에 규칙을 반영하라고 명시적으로 승인했다.
- `AGENTS.md`의 공용 규칙은 특정 도메인 사례에 고정하지 않고 일반 개념으로 작성해야 한다.
- 기존 `AGENTS.md`는 설명 요청에 비유, 정의, 비교, 구체 예시, 레퍼런스를 요구하지만 요약·리포트 전반의 독자 관점 규칙은 별도로 없다.
- 과제 제출물 1은 최소 400자이며 현재 약 1,000자로 조건을 충족한다.
- 근거: 현재 `AGENTS.md`, `task3/assignments/taskC-1.md`, Jakarta Persistence 3.2 명세, Hibernate ORM 6.6 User Guide, Microsoft N-tier Architecture Style.
- 변경 후 UTF-8 재확인, Markdown 구조 확인, `git diff --check`를 수행한다.

# 제약

- 사용자 요청과 무관한 규칙은 수정하지 않는다.
- 규칙 본문에는 특정 도메인 명사를 사용하지 않는다.
- 예시를 추가하되 기존 비평의 핵심 주장과 조건을 바꾸지 않는다.
- `@Entity` 자체를 문제로 단정하지 않고 영속성 모델과 생명주기가 상위 계층으로 전파되는 경우를 누수로 다룬다.
