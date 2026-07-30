# Task B-6: AI 파이프라인 (SOLID 위반 탐지 + 리팩토링 제안 에이전트)

(Grit's Why): 이것이 이번 Station의 진짜 산출물입니다. 1-1의 테스트 생성 파이프라인 구조(Gen/Critique/Eval/Validate)를 코드 품질 도메인으로 확장합니다. 처음부터 다시 만들지 마세요. (1-1을 닫았다면 이미 하네스가 있으니 확장하면 됩니다. 1-1을 병행 마감 중이라 아직 하네스가 없다면, B-6 전에 v0부터 세우는 것까지가 범위입니다.)

### 파이프라인 4단 (Gen과 Eval 분리 유지)

- Step 1 Gen: 코드를 입력하면 SOLID 위반 후보와 리팩토링 제안(Rich Domain 전환 포함)을 뽑는 에이전트.
- Step 2 Critique: 새 세션에 '시니어 리뷰어' 역할을 주고, 제안이 과설계(over-engineering)는 아닌지, 빠진 위반은 없는지 지적.
- Step 3 Eval: 루브릭으로 평가 (위반 진단 정확도, 변경 최소성, 행위 보존 위험도, 테스트 용이성 개선).
- Step 4 Validate: 'should be cleaner' 같은 검증 불가능한 제안을 금지 패턴으로 거르고, 적용 후 1-1 테스트가 전부 GREEN인지 자동 확인.

### 수행 내용

1. 1-1 하네스를 재사용해 코드 품질 파이프라인으로 확장해 주세요. 어디를 재사용했고 무엇을 새로 만들었는지 적어 주세요.
2. B-2의 죽은 코드를 파이프라인에 통과시키고, 파이프라인의 제안과 본인이 B-5에서 손으로 한 리팩토링을 나란히 비교해 주세요. 어느 쪽이 더 나았고 왜인지.
3. 실행 로그를 보존하고, 파이프라인이 과설계를 제안한 사례가 있었는지 기록해 주세요.

### 제출물

- [x] 파이프라인 코드/프롬프트/루브릭을 GitHub에.
  - 스킬 전체: [.codex/skills/refactor-agent](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/refactor-agent)
  - runner·게이트: [runner.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/refactor-agent/pipeline/runner.py) · [behavior_gate.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/refactor-agent/pipeline/behavior_gate.py) — Diagnose→Implement→Validate→Critique∥Eval 오케스트레이션 + worktree 행위 게이트.
  - 프롬프트: [prompts/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/refactor-agent/pipeline/prompts) (diagnose·implement·critique·eval)
  - 루브릭: [refactor.rubric.json](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/refactor-agent/pipeline/rubrics/refactor.rubric.json) — 4축 사다리 + 결함 상한(caps).
  - 공유 참조: [smell-solid-map.md](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/refactor-agent/references/smell-solid-map.md) — 파울러 24 스멜 + A/B/C 게이트 + SOLID 매핑.
  - 설계 문서(재사용 vs 신규 = 수행내용 #1 포함): [docs/design-v0.md](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/refactor-agent/docs/design-v0.md)
- [x] 파이프라인 제안 vs 본인 수작업 리팩토링 비교 정리.
  - [comparison-b2-b5.md](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/refactor-agent/runs/c0/comparison-b2-b5.md) — 정량 지표·축별 대조·행위보존 방법 차이·B-2 비평 연결·판정.
  - 파이프라인 결과 코드: [runs/c0-strict/artifact](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/refactor-agent/runs/c0-strict/artifact)
- [x] 실행 로그 + 과설계 제안 사례 메모.
  - 실행 로그(설계 iter별): [iter_001](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/refactor-agent/runs/c0-strict/iter_001) · [iter_002](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/refactor-agent/runs/c0-strict/iter_002) (diagnose·implement·gate·critique·eval) + final.json.
  - 과설계 사례: 비교 정리의 "과설계 사례 기록" 절 — OCP 결제 Strategy는 억제(v&lt;2 → DEFER), refine 과교정으로 `of()` 팩토리(Type C) 1건 발생·Critique 포착.

---

## 답안

### 1. 어디를 재사용했고 무엇을 새로 만들었나

1-1의 generate-test 하네스 골격을  가져왔다. 재사용한 것은 루프 제어 구조, 스키마 검증과 역할경계
금지필드, 금지패턴 가드 → refine 배관, 루브릭 로더, 파일 매니페스트 승격, 정보 차단, LLM 클라이언트다.
새로 만든 것은 diagnose·implement·critique·eval 프롬프트, 4축 루브릭, 스멜·SOLID 참조표(파울러 24 스멜
카탈로그는 B-5의 refactoring-criteria에서 복사), 그리고 핵심인 worktree 행위 게이트(`behavior_gate.py`)다 —
이건 generate-test엔 없다. 상세는 design-v0.md §1에 표로 있다.

Gen과 Eval 분리는 지키되, "Gen"을 진단(Diagnose)과 구현(Implement)으로 쪼갰다. 진단(판단)과 구현(변환)은
변경 이유가 다른 책임이라 나눴고, Critique/Eval이 제안이 아니라 실제 구현된 코드를 보고 판단하도록 순서를 잡아
실제로는 5단(Diagnose → Implement → Validate → Critique∥Eval)이 됐다.

### 2. 어느 쪽이 더 나았나, 왜

완성도와 규율은 손 리팩토링(B-5)이 나은 것 같다. B-5는 6개 기법을 한 커밋 한 기법으로 쪼갰고, 불변식을 도메인
객체 안에 내재화했고, 과교정을 안 했다. 파이프라인은 진단 일치(#9·#22·#6·#3·#24)·행위 보존·YAGNI에서 대등했고
훨씬 빨랐으며(LLM 8콜, 수 분), 행위 보존을 worktree 게이트로 기계 검증한 점이 오히려 사람보다 객관적이다.
B-2가 짚은 문제도 그대로 반영했다 — Testability 비평(순수 규칙 검증에 Mock 전체가 필요)은 결정을 Ticket으로
옮겨 Mock 0개로 풀었다.

다만 파이프라인엔 두 한계가 보인다. 하나는 refine 2바퀴째가 과교정으로 호출처 0인 `of()` 팩토리(Type C
과설계)를 낸 것이다. 다른 하나는 **원본을 한 번 진단→구현하는 단일 패스라, 리팩토링을 하고 *나서야* 생기는
2차 정리는 못 한다**는 것이다. 예를 들어 추출로 생긴 중복(#2)은 원본엔 없다가 추출한 뒤에야 생기는데, B-5는
C1·C2로 추출한 다음 C6에서 그 중복을 정리하는 식으로 자기가 만든 결과를 다음 커밋이 이어서 손봤다. 파이프라인의
refine 루프는 품질 미달을 고치는 되먹임이지 성공한 리팩토링 위에 다음 리팩토링을 쌓는 게 아니라, 이걸 자동화하려면
PASS한 결과를 새 baseline으로 다시 태우는 다회차가 필요할 것 같다.

### 3. 실행 로그 + 과설계 사례

실행 로그는 `runs/c0-strict/iter_001·iter_002`에 각 단계(diagnose·implement·gate·critique·eval + final)별로 남겼다.

**파이프라인을 2번 돌렸다.** 1바퀴째는 Eval이 "테스트 용이성"을 낮게(3점) 매겨 탈락시켰고, 2바퀴째에 다시 다듬어 불필요한 주석(파울러 스멜 #24)까지 잡고 통과했다.

**과설계 예**

- 다듬는 과정에서 AI가 **부르는 곳이 하나도 없는** `of()` 팩토리 메서드를 만들었다.

### 4. 회고

구현하면서 제일 걸린 건 2차 제안을 어떻게 잡느냐다. 리팩토링은 하고 나서야 새 정리거리가 생기는데(추출로 생긴
중복 같은 것), 지금 구조로는 세 갈래가 다 막혀 있었다.

- 리팩토링을 **제안만** 하면, 실제로 적용을 안 해보니 적용 후에 생기는 2차 제안을 못 잡는다.
- 그렇다고 **refine을 gen(진단)부터** 다시 돌려도, 진단이 보는 건 여전히 원본이라 2차는 안 보인다.
- 그럼 **gen을 건너뛰고 tdd처럼 계속 진행**하자니, 초기에 잘못한 걸 제대로 못 고친 채 그 위에 새 리팩토링이
쌓여서 코드가 엉망이 된다.

결국 2차를 못 보거나(앞의 둘), 보려고 계속 가면 잘못된 걸 안고 가서 엉키거나(마지막) 둘 중 하나라, 어떻게 할지
고민이다.

생각나는 방향은 둘이다.

1. 비용이 더 들더라도 **제안 루프와 구현 루프를 분리**한다. 제안(설계)을 독립적으로 돌려 수렴시킨 뒤 구현으로
기는 식이다. 대신 호출이 늘어 비용이 커진다.
2. **구현은 먼저 하되, critique/eval에서 갈래를 튼다.** 기존 리팩토링이 잘못된 게 있으면 gen(원본)부터 다시 하고,
못된 게 없으면 수정된 codebase를 새 baseline으로 삼아 거기서 다시 gen을 돌린다. 뒤쪽이 2차를 잡는 길이고,
못됐을 땐 원본으로 되돌아가니 엉망이 되는 것도 막는다.

둘 다 아직 안 해봤고 다음 스텝으로 시도해봐야겠다. 1번은 비용이 늘고, 2번은 매번 "이 리팩토링이 잘못됐나"를
critique/eval이 제대로 갈라줘야 성립한다. 어느 쪽이 될지는 돌려봐야 알 것 같다.

<!-- notion-feedback:begin -->

---

## 리뷰 피드백 (Notion 원본)

<!--
  출처 페이지 : [Phase 1] 1-2(객체지향) 제출 - 현웅님
  URL        : https://sponge-girdle-ad1.notion.site/Phase-1-1-2-38a6276f9e0081c8900dc7d1e58c8ad3
  수집 방법   : 프로젝트 루트 notion_mcp.md 참조
  원문 보존   : 댓글 본문은 Notion comment 레코드의 텍스트를 그대로 옮긴 것이며 일절 수정하지 않았다.
  라인 기준   : 이 섹션 위쪽 본문의 라인 번호. 본문을 편집하면 다시 수집해야 한다.
-->

리뷰어가 이 문서의 **어느 라인, 어떤 부분**에 **어떤 피드백**을 남겼는지 정리한 것이다.
총 4건 (댓글 4건, 리액션만 0건).

### FB-B6-01 · L52–L54

- **위치**: L52–L54
- **지적된 부분**: 문단 전체 — 완성도와 규율은 손 리팩토링(B-5)이 나은 것 같다. B-5는 6개 기법을 한 커밋 한 기법으로 쪼갰고, 불변식을 도메인 객체 안에 내재화했고, 과교정을 안 했다. 파이프라인은 진단 일치(#9·#22·#6·#3·#24)·행위 보존·YAGNI에서 대등했고 훨씬 빨랐으며(LLM 8콜, 수 분), 행위 보존을 worktree …
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 22:14 KST
- **피드백 원문**:

```
worktree 게이트가 사람보다 객관적이라는 말은 행위 보존까지만 참이 아닐까요? 더 나아졌다는 판정은 Eval 루브릭, 즉 LLM 점수인데, LLM이 만든 코드를 LLM이 채점하면 순환이 생기지 않나요?
```

### FB-B6-02 · L55–L61

- **위치**: L55–L61
- **지적된 부분**: 문단 전체 — B-2가 짚은 문제도 그대로 반영했다 — Testability 비평(순수 규칙 검증에 Mock 전체가 필요)은 결정을 Ticket으로 옮겨 Mock 0개로 풀었다.다만 파이프라인엔 두 한계가 보인다. 하나는 refine 2바퀴째가 과교정으로 호출처 0인 of() 팩토리(Type C 과설계)를 낸 것이다. 다른 하나는 원본 …
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 22:15 KST
- **피드백 원문**:

```
이 부분에서 of 메서드를 불필요하게 만든 것에 대해서 현웅님은 어떻게 생각하시나요 ? 현웅님의 의견이 궁금합니다.
```

### FB-B6-03 · L77–L78

- **위치**: L77–L78
- **지적된 부분**: 문단 전체 — 구현하면서 제일 걸린 건 2차 제안을 어떻게 잡느냐다. 리팩토링은 하고 나서야 새 정리거리가 생기는데(추출로 생긴 중복 같은 것), 지금 구조로는 세 갈래가 다 막혀 있었다.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 22:16 KST
- **피드백 원문**:

```
파울러의 리듬 자체를 파이프라인에 이식하는 것은 어떤가요? 즉 한 번에 기법 하나만 적용하고 커밋하는 단위를 게이트로 강제하는 방식이요. 

2차 정리 문제가 사실 커밋 단위가 너무 크다는 문제의 다른 이름이라면, 걸음을 줄이는 게 루프를 늘리는 것보다 먼저 아닐까 싶기도 하네요.
```

### FB-B6-04 · L96–L97

- **위치**: L96–L97
- **지적된 부분**: 문단 전체 — 둘 다 아직 안 해봤고 다음 스텝으로 시도해봐야겠다. 1번은 비용이 늘고, 2번은 매번 "이 리팩토링이 잘못됐나"를critique/eval이 제대로 갈라줘야 성립한다. 어느 쪽이 될지는 돌려봐야 알 것 같다.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 22:17 KST
- **피드백 원문**:

```
근본적으로 애초에 Gen이 생성하는 파이프라인 자체의 퀄리티를 높게 만드는 방법, 내부적인 검수 방법도 중요할 겁니다.
```

