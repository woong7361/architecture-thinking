# Review Case: subscription-apple-webhook-design

## 목적

외부 공식 문서 확인, 보안 검증, 중복 이벤트 처리, 재시도, 설계 대안 비교가 필요한 답변을 verifier가 제대로 걸러내는지 확인한다.

## 파일

- `input.md`: 실행 대상 agent에게 전달하는 사용자 요청. 기대 판정이나 정답 기준을 포함하지 않는다.
- `expected.md`: 실행 후 사람이 비교하는 기대 조건. 실행 대상 agent에게 전달하지 않는다.

## 실행 규칙

실행 대상 agent에는 `input.md`만 전달한다.

`expected.md`는 결과 비교와 디버깅에만 사용한다.
