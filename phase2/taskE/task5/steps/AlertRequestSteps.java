package com.thinking.tennis.contract;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;

/**
 * alert-request.feature 의 첫 시나리오 하나를 계약에 묶은 예시다.
 *
 * feature 가 정한 것은 무엇이 일어나는지이고, 여기서 그것이 HTTP 로 어떻게 보이는지를 정한다.
 * 대응의 출처는 api/tennis-alert-api.yaml 이며, 아래 표가 이 시나리오가 쓰는 전부다.
 *
 *   신청이 만들어지고 감시 중이다   201, status=WATCHING, Location=/v1/alerts/{alertId}
 *   이 응답은 재생이 아니다        Idempotency-Replayed: false
 *   확인에 성공한 적 없다          lastCheckedAt 키가 있고 값이 null
 *   발송 결과가 없다              delivery 키가 있고 값이 null
 *   만료 시각                    expiresAt (코트 지역 시각을 UTC 로 적은 값)
 *   신청은 N건이다               GET /v1/alerts 의 items 크기
 *
 * 구현 클래스는 참조하지 않는다. 참조하면 AI 가 짤 구조를 테스트가 미리 정하게 되고,
 * 이 테스트가 계약에서만 파생됐다는 근거도 사라진다.
 */
public class AlertRequestSteps {

    private final ContractClient client;   // HTTP 표면에만 말을 거는 얇은 클라이언트
    private final Fixtures fixtures;       // 예약처 스텁과 코트 운영 시간대
    private final MutableClock clock;      // 계약의 시각 경계를 밀고 당긴다
    private ContractResponse response;

    public AlertRequestSteps(ContractClient client, Fixtures fixtures, MutableClock clock) {
        this.client = client;
        this.fixtures = fixtures;
        this.clock = clock;
        this.response = null;
    }

    @먼저("서비스는 양재 1번 코트를 지원하고 강남 3번 코트는 지원하지 않는다")
    public void 지원_코트를_정한다() {
        fixtures.supportCourt("seoul-yangjae-1", "양재 시민의 숲 테니스장 1번");
        fixtures.unsupportCourt("seoul-gangnam-3");
    }

    @먼저("^양재 1번 코트는 (\\S+)에 (\\S+)과 (\\S+) 두 시간대를 운영한다$")
    public void 운영_시간대를_정한다(String date, String first, String second) {
        fixtures.operatingSlots("seoul-yangjae-1", date, first, second);
    }

    @먼저("^현재 시각은 (.+)이다$")
    public void 현재_시각을_정한다(String localDateTime) {
        clock.setTo(fixtures.toInstant(localDateTime));
    }

    @먼저("^(\\S+)에게 저장된 신청은 하나도 없다$")
    public void 신청이_없다(String user) {
        assertThat(client.listAlerts(user).items()).isEmpty();
    }

    @만일("^(\\S+)이 재시도 표식 (\\S+)로 양재 1번 코트의 (\\S+) (\\S+)을 신청한다$")
    public void 신청한다(String user, String key, String date, String slot) {
        response = client.createAlert(user, key, "seoul-yangjae-1", date, slot);
    }

    @그러면("신청이 만들어지고 그 신청은 감시 중이다")
    public void 감시가_시작된다() {
        assertThat(response.status()).isEqualTo(201);
        assertThat(response.json().text("status")).isEqualTo("WATCHING");
        assertThat(response.header("Location")).isEqualTo("/v1/alerts/" + response.json().text("alertId"));
    }

    @그러면("^그 신청이 감시하는 대상은 양재 1번 코트의 (\\S+) (\\S+)이다$")
    public void 감시_대상이_같다(String date, String slot) {
        assertThat(response.json().text("courtId")).isEqualTo("seoul-yangjae-1");
        assertThat(response.json().text("date")).isEqualTo(date);
        assertThat(response.json().text("slot.startTime") + "~" + response.json().text("slot.endTime"))
                .isEqualTo(slot);
    }

    @그러면("^그 신청의 만료 시각은 (.+)이다$")
    public void 만료_시각이_같다(String localDateTime) {
        assertThat(response.json().text("expiresAt")).isEqualTo(fixtures.toUtcText(localDateTime));
    }

    /**
     * 계약은 이 둘을 required 로 두고 null 을 허용했다. 없어도 된다가 아니라 키가 있고 값이 null 이라는 뜻이다.
     * 직렬화 설정이 null 을 지우면 구현은 이 계약을 어기는데, 객체로 되돌려 비교하면 그 차이가 보이지 않는다.
     * 그래서 원문 JSON 에 대고 키의 존재와 값을 따로 본다.
     */
    @그러면("그 신청은 아직 한 번도 확인에 성공하지 못했고 발송 결과도 없다")
    public void 확인도_발송도_없다() {
        assertThat(response.json().hasKey("lastCheckedAt")).isTrue();
        assertThat(response.json().isNull("lastCheckedAt")).isTrue();
        assertThat(response.json().hasKey("delivery")).isTrue();
        assertThat(response.json().isNull("delivery")).isTrue();
    }

    @그러면("이 응답은 재생이 아니다")
    public void 재생이_아니다() {
        assertThat(response.header("Idempotency-Replayed")).isEqualTo("false");
    }

    @그러면("^(\\S+)의 신청은 (\\d+)건이다$")
    public void 신청_건수가_같다(String user, int count) {
        assertThat(client.listAlerts(user).items()).hasSize(count);
    }
}
