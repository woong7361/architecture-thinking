# Original User Input

# Original User Input

task3\assignments\taskC-6.md 에서 

잡은 것은 게이트가 아니라 notes 였다.



결제사의 엔드포인트와 스키마가 이 층 입력에 없다. /payments, 2xx=승인으로 가정했다.
심판 구성에서는 결제가 계속 대역으로 남아 이 가정이 검증되지 않으므로, 실제 계약을 아는 사람이 확인해야 한다.

이 문장을 보고 스텁 매핑 파일을 열었고, 거기서 "거절도 200"이라는 AI가 예상조차 못한 부분이 드러났다.
notes 는 단서였지 판정이 아니었다.

그리고 원인은 AI가 아니라 내 입력이었다. L1 지시에 결제사 계약을 빼놓고 "없는 것을 지어내지 마라"고 한 것은
지어낼 수밖에 없는 상태로 준 것이다. 그래서 산출물을 손보지 않고 지시를 고쳐 재생성했다.
결과물을 사람이 고치면 그 층의 판정이 무의미해지고 같은 실수가 다음 실행에서 되풀이된다.
수정한 지시에는 계약을 명시하고 "이 자리는 인수테스트가 검증하지 않으니 추측하지 말라"는 경고를 함께 넣었다.

2차 산출물은 /charge 로 보내고 본문 approved 를 읽는다. 그리고 이번에는 코드를 읽는 데 그치지 않고
docker compose up 후 declined-card 를 실제로 보내 402 + problem+json 을 확인했다.

이거 

```
수정한. 지시에는 계약을 명시하고 "이 자리는 인수테스트가 검증하지 않으니 추측하지 말라"는 경고를 함께 넣었다.
```

이거 무슨소리야?
 
왜 인수테스트가 검증 안해?
 해야하는거 아니야?
 
외부 pg
계약이라 못하는거야?



# Checked Context

# Project Context

- `task3/assignments/taskC-6.md`는 L1 1차 결제 어댑터가 `/payments`와 2xx 승인 판정을 추측했지만 모든 게이트를 통과했다고 기록한다.
- `.codex/skills/skeleton-agent/pipeline/inputs/c6-ticket-skeleton.json`의 `protocol` 구성 설명은 실제 HTTP와 실제 MySQL을 사용하지만 결제 하나는 대역으로 남는다고 명시한다.
- `task3/ticket-reservation-c6/src/test/java/com/thinking/ticket/e2e/E2eCucumberSpringConfiguration.java`는 `TestPaymentConfig`를 import한다.
- `task3/ticket-reservation-c6/src/test/java/com/thinking/ticket/jpa/TestPaymentConfig.java`의 `TestChargePort`가 `ChargePort`를 구현하고 `@Primary`로 등록되어 실제 `PgChargeAdapter`를 우회한다.
- `task3/assignments/taskC-6.md`의 과제 원문은 walking skeleton 경로를 Inbound Adapter → Inbound Port → Core → Outbound Port → Outbound Adapter(Testcontainers DB)로 요구한다. 외부 PG까지 실물로 연결하라고 명시하지 않는다.
- `task3/ticket-reservation-c6/docker/mock-pg/mappings/charge-declined.json`은 POST `/charge`와 응답 `200`, 본문 `{approved:false}`를 정의한다.
- `task3/ticket-reservation-c6/README.md`는 docker compose 기반 수동 스모크를 결제 왕복의 유일한 확인이라고 명시한다.
- 결론에서 구분할 것: 현재 Cucumber 인수테스트의 업무 행위 검증, 결제 HTTP 어댑터의 프로토콜 계약 검증, 실제 외부 PG 제공자 계약의 진실성 검증은 서로 다른 범위다.
