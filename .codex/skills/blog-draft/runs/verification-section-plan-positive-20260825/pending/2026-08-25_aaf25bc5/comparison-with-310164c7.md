# `spine` 초안과 `section_plan` 초안 비교

## 결론

이번 1회 A/B 실행에서는 `section_plan`이 소제목과 절의 역할을 정렬했다는 개선 신호가 확인됐다. 다만 각 조건을 한 번씩만 실행했으므로 점수 차이를 `section_plan`의 인과 효과로 확정하지 않는다.

## 비교 조건

- 이전 input: `.codex/skills/blog-draft/pipeline/inputs/310164c7_input.json`
- 새 input: `.codex/skills/blog-draft/pipeline/inputs/section-plan-verification-20260825/aaf25bc5_input.json`
- 같은 것: raw text, reader, guide, judgment, constraints, provider, model, rubric, 반복 상한
- 다른 것: `spine` 9개를 `section_plan` 9개로 교체

## 수치

| 항목 | 이전 `spine` | 새 `section_plan` | 차이 |
| --- | ---: | ---: | ---: |
| 품질 게이트 | REJECT | PASS | - |
| 총점 | 4.142 | 4.331 | +0.189 |
| structure | 3.8 | 4.3 | +0.5 |
| sentence | 3.7 | 4.1 | +0.4 |
| 본문 길이 | 5,577자 | 5,682자 | +105자 |
| H2 | 10개 | 9개 | -1개 |

다른 품질 축도 모두 0.1~0.2점 올랐다. 이는 보조 신호로만 본다.

## 구조 비교

새 Critique에는 9개의 `section_reviews`가 생성됐다.

- 9개 절 모두 `heading_match`, `purpose_match`, `material_use_match` 통과
- 선언한 절 연결 7개 모두 `matched`
- 선언하지 않은 `s7`, `s9`는 `not_declared`
- 입력에 없는 주장 0개

이전 Critique의 핵심 지적이었던 소제목과 본문 불일치, 여러 사례의 혼재는 대폭 줄었다. 새 소제목도 추상 개념보다 해당 절에서 벌어진 일을 직접 약속한다.

## 남은 문제

### `s4`

점수 흔들림을 다루는 절에 첫 채점표 수정과 문자열 검사로 내린 사례가 추가로 들어갔다. 문자열 검사 사례는 `must_include`지만 section plan에 독립된 자리가 없었다. 현재 근거로는 이 계약 충돌 때문에 생성기가 `s4`에 넣었을 가능성이 크다.

### `s9`

purpose에 사람의 책임, 성장의 정의와 대가, 깨지는 조건, 다음 검증을 함께 넣었다. 결과적으로 마지막 절에 한계가 다시 몰렸다. 이 문제는 생성 prompt보다 section plan purpose를 너무 넓게 쓴 영향이 크다.

## 다음 수정 순서

1. 문자열 검사로 내린 사례에 독립 절을 준다.
2. `s9`를 사람에게 남은 일과 바로 다음 검증으로 좁힌다.
3. 현재 rubric은 유지한 채 한 번 더 실행한다.

prompt 수정은 전체 생성에 영향을 주므로 범위가 넓다. rubric 수정은 이번 범위 이탈의 원인을 직접 고치지 않고 비교 자만 바꿄다. 문제가 난 절의 계약만 좁히는 section plan 수정이 가장 작고 직접적인 다음 실험이다.
