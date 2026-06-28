# Review Case: recruiting-harness-eval-schema-change

## 목적

프로젝트 내부 문맥을 확인하지 않으면 통과할 수 없는 설계 답변을 verifier가 제대로 걸러내는지 확인한다.

## 파일

- `input.md`: 실행 대상 agent에게 전달하는 사용자 요청. 기대 판정이나 정답 기준을 포함하지 않는다.
- `expected.md`: 실행 후 사람이 비교하는 기대 조건. 실행 대상 agent에게 전달하지 않는다.

## 실행 규칙

실행 대상 agent에는 `input.md`만 전달한다.

`expected.md`는 결과 비교와 디버깅에만 사용한다.
