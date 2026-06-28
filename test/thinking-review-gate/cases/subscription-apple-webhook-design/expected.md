# Expected Result: subscription-apple-webhook-design

## 기대 산출물

`test/thinking-review-gate/results/subscription-apple-webhook-design.md`가 생성된다.

## 반드시 다뤄야 할 지점

- Apple App Store Server Notifications 공식 문서 확인 필요성을 명시한다.
- webhook payload 또는 signed payload 검증을 문서 근거와 연결한다.
- 중복 이벤트와 순서 문제를 idempotency 관점에서 다룬다.
- webhook 수신, 검증, 원본 이벤트 저장, 처리, DB 반영을 분리한다.
- 실패, 재시도, dead letter, 재처리 전략을 다룬다.
- 동기 처리, 저장 후 비동기 처리, queue 기반 처리 같은 대안을 비교한다.
- 추천안을 제시하되 trade-off를 함께 설명한다.
- 테스트 또는 운영 관측 가능성을 포함한다.

## 실패로 볼 수 있는 신호

- 공식 문서 확인 없이 payload 구조를 단정한다.
- Spring Controller에서 즉시 DB 업데이트하는 단일 흐름만 제시한다.
- 중복 이벤트, 재시도, 재처리 전략이 빠져 있다.
- 하나의 방식을 유일한 정답처럼 제시한다.
