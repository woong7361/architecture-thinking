# Project Context

- `task3/assignments/taskC-6.md`는 L1 1차 결제 어댑터가 `/payments`와 2xx 승인 판정을 추측했지만 모든 게이트를 통과했다고 기록한다.
- `.codex/skills/skeleton-agent/pipeline/inputs/c6-ticket-skeleton.json`의 `protocol` 구성 설명은 실제 HTTP와 실제 MySQL을 사용하지만 결제 하나는 대역으로 남는다고 명시한다.
- `task3/ticket-reservation-c6/src/test/java/com/thinking/ticket/e2e/E2eCucumberSpringConfiguration.java`는 `TestPaymentConfig`를 import한다.
- `task3/ticket-reservation-c6/src/test/java/com/thinking/ticket/jpa/TestPaymentConfig.java`의 `TestChargePort`가 `ChargePort`를 구현하고 `@Primary`로 등록되어 실제 `PgChargeAdapter`를 우회한다.
- `task3/assignments/taskC-6.md`의 과제 원문은 walking skeleton 경로를 Inbound Adapter → Inbound Port → Core → Outbound Port → Outbound Adapter(Testcontainers DB)로 요구한다. 외부 PG까지 실물로 연결하라고 명시하지 않는다.
- `task3/ticket-reservation-c6/docker/mock-pg/mappings/charge-declined.json`은 POST `/charge`와 응답 `200`, 본문 `{approved:false}`를 정의한다.
- `task3/ticket-reservation-c6/README.md`는 docker compose 기반 수동 스모크를 결제 왕복의 유일한 확인이라고 명시한다.
- 결론에서 구분할 것: 현재 Cucumber 인수테스트의 업무 행위 검증, 결제 HTTP 어댑터의 프로토콜 계약 검증, 실제 외부 PG 제공자 계약의 진실성 검증은 서로 다른 범위다.

