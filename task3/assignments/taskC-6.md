# Task C-6: Walking Skeleton + AI 파이프라인

(Grit's Why): 이것이 이번 Station의 진짜 산출물입니다. C-3에서 설계하고 C-5에서 Port 레벨 인수테스트를 초록불로 만들었다면, C-6은 마지막 간극을 닫습니다. 실제 Inbound Adapter(컨트롤러)까지 붙여 HTTP부터 DB까지 진짜 한 줄기로 통하는, 끝에서 끝까지 가장 얇게 도는 walking skeleton을 만들고, 그 골격 생성을 AI 파이프라인으로 돌립니다.

### 수행 내용

1. 인수테스트 1개가 통과하는 최소 end-to-end 골격(walking skeleton)을 세우세요. Inbound Adapter(예: 컨트롤러) → Inbound Port → Core → Outbound Port → Outbound Adapter(Testcontainers DB)까지 한 줄기가 실제로 도는 상태.
2. 이 골격 생성을 AI에게 맡기되, 1-1/1-2의 하네스를 확장한 파이프라인으로 운영하세요. agent.md/CLAUDE.md에 헥사고날 컨벤션(포트/어댑터 규약, 의존성 방향)을 컨텍스트로 주고, Layer 단위로 지시·검수(Iterative Prompting)하세요. 매 단계 Cucumber 인수테스트로 결과를 판정합니다.
3. AI가 만든 골격을 본인이 검수한 기록(수용/기각 + 이유)을 남기세요. 헥사고날 경계를 어긴 제안(예: Core가 Adapter를 직접 의존)을 잡아낸 사례가 있으면 적으세요.

### 제출물

- [x] walking skeleton 코드(1 인수테스트 초록불, 1-command 기동)를 GitHub에.
- [x] AI 파이프라인(프롬프트/컨텍스트/agent.md)과 Layer 단위 검수 로그.
- [x] AI 제안 중 헥사고날 경계 위반을 잡아낸 사례 + 본인 판단. (위반이 없었다면, 검수 시 적용한 경계 점검과 AI 출력이 경계를 지킨 이유를 적으세요.) (최소 300자)

---

## 개요

**골격의 `src/main` 을 전부 AI에게 층 단위로 만들게 하고, 층마다 인수테스트로 판정했다.**

계약(도메인·포트)과 심판(인수테스트)은 사람이 고정하고, 그 사이를 채우는 네 층을 순서대로 생성시켰다.

```
L0 유스케이스 → L1 아웃바운드 어댑터 → L2 조립 → L3 인바운드 어댑터
   각 층마다: 경로 규약 → 컴파일 → 경계 규칙 → 인수테스트
```

## 산출물 위치


|                     | 위치                                                                                                                                                                                            |
| ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| walking skeleton 코드 | [`../ticket-reservation-c6/`](https://github.com/woong7361/architecture-thinking/tree/main/task3/ticket-reservation-c6)                                                                       |
| 파이프라인               | [`../../.codex/skills/skeleton-agent/pipeline/`](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/skeleton-agent/pipeline)                                          |
| 공통 AI 컨벤션           | [`skeleton-agent/CLAUDE.md`](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/skeleton-agent/CLAUDE.md)                                                             |
| C-6 run 입력·구체 컨텍스트  | [`skeleton-agent/pipeline/inputs/c6-ticket-skeleton.json`](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/skeleton-agent/pipeline/inputs/c6-ticket-skeleton.json) |
| 공용 레이어 지시 프롬프트      | [`skeleton-agent/pipeline/prompts/layers/`](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/skeleton-agent/pipeline/prompts/layers)                                |
| 설계 문서               | [`skeleton-agent/docs/design-v0.md`](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/skeleton-agent/docs/design-v0.md)                                             |
| 헥사고날 컨벤션(AI 컨텍스트)   | [`ticket-reservation-c6/CLAUDE.md`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation-c6/CLAUDE.md)                                                       |
| 층별 검수 로그            | [`skeleton-agent/runs/c6/`](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/skeleton-agent/runs/c6)                                                                |
| 자동 검사 비교            | [`runs/c6/diff-vs-handwritten.md`](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/skeleton-agent/runs/c6/diff-vs-handwritten.md)                                  |


---

## 제출물 3 — 경계 위반 사례와 본인 판단

### DB 의존 코드

먼저 핵심 유스케이스인 `TicketService` 는 괜찮았다. 이 클래스는 실제 저장소가 JPA인지, 요청이 HTTP로 들어왔는지 모른다. 필요한 기능을 포트로만 요청한다.

```java
public class TicketService implements ReserveTicketUseCase {

    private final LoadTicketPort loadTicketPort;
    private final SaveTicketPort saveTicketPort;
    private final LoadUserPort loadUserPort;
    private final ChargePort chargePort;
    private final DiscountPolicy discountPolicy;
}
```

웹 컨트롤러도 괜찮았다. `TicketService` 를 직접 부르지 않고, `ReserveTicketUseCase` 라는 포트를 통해 호출한다.

```java
public class ReservationController {

    private final ReserveTicketUseCase reserveTicketUseCase;

    public ReservationController(ReserveTicketUseCase reserveTicketUseCase) {
        this.reserveTicketUseCase = reserveTicketUseCase;
    }
}
```

문제 후보는 예외 처리 코드에서 나왔다.

```java
import org.springframework.dao.OptimisticLockingFailureException;

@ExceptionHandler(OptimisticLockingFailureException.class)
public ResponseEntity<ProblemDetail> handleLockingFailure(OptimisticLockingFailureException e) {
    return problem(HttpStatus.CONFLICT, "다른 요청이 먼저 예약을 확정했습니다.");
}
```

`OptimisticLockingFailureException` 은 예매 업무의 말이 아니다. Spring 저장소 기술에서 나오는 예외다. 쉽게 말하면 웹 계층이 "DB 저장 중 어떤 기술 예외가 났는지"까지 알고 있는 상태다.

더 나은 구조는 저장 어댑터 안에서 이 예외를 예매 도메인의 예외로 바꿔서 올리는 것이다.

```java
try {
    int updated = ticketJpaRepository.reserveIfNotReserved(ticket.getId(), ticket.getUserId());
    if (updated == 0) {
        throw new TicketAlreadyReservedException();
    }
} catch (OptimisticLockingFailureException e) {
    throw new TicketAlreadyReservedException();
}
```

그러면 웹 어댑터는 Spring 저장소 예외를 몰라도 된다. 이미 있는 예매 도메인 예외만 HTTP 응답으로 바꾸면 된다.

```java
@ExceptionHandler(TicketAlreadyReservedException.class)
public ResponseEntity<ProblemDetail> handleAlreadyReserved(TicketAlreadyReservedException e) {
    return problem(HttpStatus.CONFLICT, "이미 예약된 티켓입니다.");
}
```

이 문제는 현재 테스트를 깨뜨리지는 않는다. 하지만 나중에 저장 기술을 바꾸면 웹 코드까지 함께 고쳐야 할 수 있다. 그래서 이 지점은 **테스트는 통과했지만 사람이 코드 리뷰로 발견한 작은 경계 누수**로 기록한다.

### 게이트를 통과한 틀린 코드

L1 1차 산출물은 **게이트 4단을 전부 통과했고 인수테스트 21개가 초록불이었는데 틀렸다.**

```java
this.chargeUri = URI.create(baseUrl + "/payments");        // 실제 스텁은 /charge
return response.getStatusCode().is2xxSuccessful();         // 스텁은 거절도 200 + {"approved":false}
```

**거절된 결제가 승인으로 처리되는 코드**였다. 게이트가 못 잡은 이유는 명확하다 — 결제 슬롯은 모든 판정 구성에서 대역이라 **이 코드가 실행조차 되지 않는다.** 인수테스트가 몇 개든 통과 여부와 무관하다. "다 초록불이니 다 됐다"가 왜 틀린지의 실물 사례다.

잡은 것은 게이트가 아니라 `notes` 였다.

> 결제사의 엔드포인트와 스키마가 이 층 입력에 없다. `/payments`, 2xx=승인으로 가정했다. 심판 구성에서는 결제가 계속 대역으로 남아 **이 가정이 검증되지 않으므로**, 실제 계약을 아는 사람이 확인해야 한다.

이 문장을 보고 스텁 매핑 파일을 열었고, 거기서 "거절도 200"이라는 AI가 예상조차 못한 부분이 드러났다. `notes` 는 단서였지 판정이 아니었다.

이 실패에는 원인이 둘 있었다. **틀린 코드가 생성된 원인**은 L1 입력에서 결제사 계약을 빠뜨린 것이다. 엔드포인트와 승인 판정 기준이 없었기 때문에 생성기는 그 빈자리를 `/payments` 와 2xx 승인이라는 가정으로 채웠다. 그러나 **틀린 코드가 게이트를 통과한 원인**은 테스트 커버리지 공백이다. L1은 저장 어댑터와 결제 어댑터를 함께 생성하지만, L1의 `storage` 구성과 최종 `protocol` 구성 모두 `ChargePort` 를 테스트 더블로 교체한다. 따라서 `PgChargeAdapter` 는 어느 자동 판정에서도 실행되지 않았다. 입력 누락은 오답을 만들었고, 게이트 공백은 그 오답에 GREEN을 줬다.

이번에는 생성된 구현을 손으로 고치지 않고 L1 입력에 `/charge`, 요청 본문, 응답 본문의 `approved`, 거절도 HTTP 200이라는 계약을 명시해 재생성했다. 2차 산출물은 `/charge` 로 보내고 본문 `approved` 를 읽는다. `docker compose up` 후 `declined-card` 를 실제로 보내 mock PG가 `200 + approved:false` 를 반환하고 애플리케이션이 이를 **402 + problem+json** 으로 변환하는 것도 확인했다. 다만 이것은 이번 결과를 확인한 **수동 스모크**이지, 같은 실수를 다음 실행에서 자동으로 막는 게이트는 아니다.

이 경험에서 **AI에게 구현을 맡길 때, 게이트가 검증하지 못한 가정과 잠재 위험을 구조화된 report로 함께 제출하게 하는 방식을 생각해보았다.** 자동 판정은 기존 테스트의 GREEN/RED로 유지하되, AI가 테스트 밖의 case를 발견하면 근거·사용한 가정·잠재 영향·확인 방법을 report한다. feature 계약은 별도 게이트에서 생성·승인하므로, 구현을 맡은 AI가 발견한 case를 임의로 계약이나 실패 테스트로 추가하지 않는다. 대신 report의 내용을 feature 계약 게이트의 후보 입력으로 보내고, 사람이 승인한 case만 다음 실행의 계약·input·테스트에 반영한다. 이번 실패라면 `PgChargeAdapter`가 자동 판정에서 실행되지 않았고 `/charge`, 요청 본문, `approved:false`의 해석이 미검증 상태라는 점이 report 대상이 된다. 이 방식은 AI의 추측이 임의로 계약이 되는 위험과, GREEN만 보고 미검증 위험을 넘기는 문제를 함께 줄일 수 있을 것이라 생각된다.

## 리뷰 피드백 (Notion 원본)

> **피드백 메타데이터**
> - 출처 페이지: [Phase 1] 1-3(헥사고날) 제출 - 현웅님
> - URL: [Notion 원본 페이지](https://sponge-girdle-ad1.notion.site/Phase-1-1-3-3a26276f9e0081b399c3f614fe445fa7)
> - 수집 방법: 프로젝트 루트 `notion_mcp.md` 참조
> - 원문 보존: 댓글 본문은 Notion comment 레코드의 텍스트를 그대로 옮긴 것이며 일절 수정하지 않았다.
> - 라인 기준: 이 섹션 위쪽 본문의 라인 번호. 본문을 편집하면 다시 수집해야 한다.

리뷰어가 이 문서의 **어느 라인, 어떤 부분**에 **어떤 피드백**을 남겼는지 정리한 것이다.
총 2건.

### FB-C6-01 · L136

- **위치**: L136
- **지적된 부분**: 이 경험에서 AI에게 구현을 맡길 때, 게이트가 검증하지 못한 가정과 잠재 위험을 구조화된 report로 함께 제출하게 하는 방식을 생각해보았다
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-08-10 16:08 KST
- **피드백 원문**:

```
네 저도 동의합니다만, report를 매번 사람이 읽어야 한다면 병목이 다시 사람이 됩니다. report 항목 중 사람의 판단 없이 자동으로 다음 게이트로 넘길 수 있는 것이 있을까요? 예를 들어 어떤 슬롯이 대역이었다는 사실은 판단이 아니라 관측인데, 그런 항목부터 자동화한다면 무엇이 남을까요?
```

**답변 초안:**

report를 `관측 사실`과 `판단이 필요한 주장`으로 나누면 자동화할 수 있는 부분이 많다고 생각합니다. 어떤 프로덕션 Adapter가 테스트에서 한 번도 실행되지 않았는지, 어떤 Port가 테스트 더블로 치환됐는지, 입력 계약에 출처가 붙어 있는지, Core가 금지된 기술 패키지를 참조하는지는 사람의 취향이 아니라 기계가 확인할 수 있는 사실입니다. 이런 항목은 report에만 남기지 않고 자동 게이트로 올려, 새로 생성된 프로덕션 Adapter의 실행 증거가 없거나 계약 출처가 비어 있으면 실패시키겠습니다.

자동 판정 뒤 사람에게 남는 것은 그 사실의 업무 의미입니다. 미실행 Adapter를 이번 배포에서 허용할지, 서로 충돌하는 계약 중 무엇을 우선할지, 실패 가능성과 출시 지연 중 어떤 위험을 감수할지는 자동화가 대신 결정하기 어렵습니다. 따라서 기계는 `무엇이 검증됐고 무엇이 비어 있는가`를 좁혀 주고, 사람은 목표와 손실을 기준으로 예외를 승인하거나 계약을 바꾸는 구조가 적절해 보입니다. 이번 사례라면 `PgChargeAdapter 실행 0회`는 자동 실패로 넘기고, 실제 PG까지 어느 수준으로 검증해야 출시할지는 사람이 결정할 항목입니다.

### FB-C6-02 · L136

- **위치**: L136
- **지적된 부분**: 이 방식은 AI의 추측이 임의로 계약이 되는 위험과, GREEN만 보고 미검증 위험을 넘기는 문제를 함께 줄일 수 있을 것이라 생각된다.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-08-10 16:09 KST
- **피드백 원문**:

```
그런데 이 설계에도 남는 위험이 하나 있습니다. 사람이 승인하는 계약 자체가 틀린 경우입니다. 이번 결제 계약도 결국 스텁 파일을 열어 확인하셨는데, 실제 PG였다면 무엇을 근거로 계약을 확정하셔야할까요? 
```

**답변 초안:**

실제 PG 계약은 사람의 기억이나 스텁 하나가 아니라 출처가 있는 증거 묶음으로 확정해야 합니다. 우선 버전과 확인 날짜가 있는 공식 API 문서와 스키마를 기준으로 삼고, PG Sandbox에서 승인·거절·타임아웃 응답을 직접 실행해 계약 테스트로 고정하겠습니다. 문서와 실행 결과가 다르면 공급자 지원 채널의 확인이나 계약 문서를 근거로 남기고, 가능하다면 운영 전 검증 환경의 실제 통신 기록도 민감정보를 제거해 대조하겠습니다.

각 계약 항목에는 `어떤 출처에서 언제 확인했는가`를 기록해 문서 버전이 바뀌면 재검증 대상이 되게 해야 합니다. 공식 문서, Sandbox, 공급자 확인이 서로 충돌하거나 결과를 확정할 근거가 없다면 성공으로 추정하지 않고 `UNKNOWN`으로 처리한 뒤 조회나 대사 흐름으로 넘기겠습니다. 사람의 역할도 계약 내용을 직감으로 승인하는 것이 아니라, 독립된 증거가 충분한지와 불확실할 때의 실패 정책이 안전한지를 승인하는 쪽으로 바뀌어야 한다고 생각합니다.
