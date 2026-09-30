package com.thinking.tennis.port;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 외부 예약처에서 코트의 예약 상태를 가져오는 포트다.
 *
 * <p><b>이 포트는 사람이 소유한다. 생성 코드는 이 시그니처에 맞춘다.</b> 시그니처가 불편하더라도
 * 고치지 말고 그 이유를 보고한다. 이 계약은 스켈레톤의 인메모리 어댑터와 게이트가 함께 의존하므로
 * 한쪽 사정으로 바꾸면 다른 쪽이 조용히 깨진다.
 *
 * <p>계약의 {@code GET /courts/{courtId}/availability} 가 필요로 하는 것만 담는다. 코트를 지원하는지,
 * 지원하면 그 코트가 어떤 코트인지, 그 날짜에 운영하는 시간대마다 마지막 확인 시점에 비어 있었는지,
 * 그 확인이 언제 성공했는지, 그리고 확인이 실패했다면 무엇이 실패했는지다.
 *
 * <p>여기에 HTTP 타입을 쓰지 않는다. {@code ResponseEntity}·{@code HttpStatus}·서블릿 타입은 인바운드
 * 어댑터의 어휘이고, 이 포트는 안쪽이 바깥에 요구하는 계약이라 바깥의 기술을 몰라야 한다.
 * 확인 실패를 상태 코드가 아니라 {@link CheckFailed} 로 표현하는 이유도 같다. 실패를 상태 코드로 옮기는
 * 판단은 계약이 정한 대로 인바운드 어댑터가 한다. 저장된 성공 결과가 있으면 200에 {@code stale} 을 세우고
 * 없으면 5xx로 나가는 갈림길이 여기서는 보이지 않는다.
 *
 * <p>확인 간격과 중복 조회 억제는 이 포트의 몫이 아니다. 이 포트는 부르면 그때 확인한다.
 * 얼마나 자주 부를지, 같은 코트·날짜로 몰린 요청을 어떻게 한 번으로 묶을지는 유스케이스가 정한다.
 */
public interface CourtAvailabilityPort {

    /**
     * 서비스가 지원하는 코트인지 확인하고, 지원하면 그 코트를 돌려준다.
     *
     * <p>지원하지 않는 코트면 빈 값이다. 계약은 이 경우를 경로에서 받았으면 404
     * {@code COURT_NOT_SUPPORTED} 로, 본문에서 받았으면 422로 내보내라고 하지만, 그 구분은 요청을 받은
     * 자리가 아는 것이라 이 포트는 지원 여부만 말한다.
     *
     * @param courtId 코트 식별자
     * @return 지원하는 코트, 지원하지 않으면 {@link Optional#empty()}
     */
    Optional<SupportedCourt> findCourt(String courtId);

    /**
     * 코트 하나의 하루치 예약 상태를 외부 예약처에서 확인한다.
     *
     * <p>예약처가 아직 그 날짜를 열지 않았으면 확인은 성공하고 {@link CourtAvailabilityCheck#slots()} 가
     * 빈 목록이다. 날짜를 열지 않은 것과 확인이 실패한 것은 다른 일이므로 구분한다.
     *
     * @param courtId 코트 식별자
     * @param date    조회할 날짜. 코트가 있는 지역의 날짜다
     * @return 확인에 성공한 결과
     * @throws CheckFailed                확인이 실패했다
     * @throws CourtNotSupportedException 지원하지 않는 코트다
     */
    CourtAvailabilityCheck checkAvailability(String courtId, LocalDate date)
            throws CheckFailed, CourtNotSupportedException;

    /**
     * 서비스가 지원하는 코트 하나다.
     *
     * @param courtId        코트 식별자
     * @param courtName      사람이 읽는 코트 이름
     * @param reservationUrl 외부 예약 사이트의 이 코트 예약 화면 주소
     */
    record SupportedCourt(String courtId, String courtName, String reservationUrl) {
    }

    /**
     * 시간대 하나와 마지막 확인 시점의 빈자리 여부다.
     *
     * <p>{@code available} 은 {@link CourtAvailabilityCheck#confirmedAt()} 시점의 사실이다.
     * 확인에 실패했을 때의 값이 아니다.
     *
     * @param startTime 시간대의 시작 시각
     * @param endTime   시간대의 끝 시각
     * @param available 그 시점에 비어 있었는지 여부
     */
    record SlotAvailability(LocalTime startTime, LocalTime endTime, boolean available) {
    }

    /**
     * 확인에 성공한 결과다.
     *
     * @param court       확인한 코트
     * @param date        확인한 날짜
     * @param confirmedAt 이 결과를 예약처에서 가져오는 데 성공한 시각
     * @param slots       그 날짜에 이 코트가 운영하는 시간대 전부. 예약처가 날짜를 열지 않았으면 빈 목록
     */
    record CourtAvailabilityCheck(SupportedCourt court,
                                  LocalDate date,
                                  Instant confirmedAt,
                                  List<SlotAvailability> slots) {
    }

    /**
     * 외부 예약처 확인이 실패한 이유다.
     *
     * <p>이름은 계약의 {@code UpstreamFailureReason} 과 같게 두었다. 저장된 성공 결과가 없었다면 같은
     * 이름의 에러 코드로 나갈 실패라서, 중간에 다른 이름을 두면 옮겨 적는 자리만 늘고 얻는 것이 없다.
     */
    enum UpstreamFailureReason {
        /** 정해진 시간 안에 응답이 오지 않았다. */
        UPSTREAM_TIMEOUT,
        /** 예약처에 닿지 못했거나 예약처가 실패를 돌려줬다. */
        UPSTREAM_UNAVAILABLE,
        /** 응답은 왔지만 내용을 읽지 못했다. */
        UPSTREAM_RESPONSE_UNREADABLE
    }

    /**
     * 외부 예약처 확인이 실패했다.
     *
     * <p>검사 예외로 둔 것은 의도다. 계약은 확인 실패를 만났을 때 저장된 마지막 성공 결과가 있으면 200에
     * {@code stale} 을 세우고 없으면 5xx로 알리라고 한다. 호출하는 쪽이 그 갈림길을 반드시 지나가게 하려고
     * 컴파일러가 처리를 요구하는 예외를 쓴다.
     */
    class CheckFailed extends Exception {

        private final UpstreamFailureReason reason;

        public CheckFailed(UpstreamFailureReason reason) {
            super("외부 예약처 확인 실패: " + reason);
            this.reason = reason;
        }

        public UpstreamFailureReason reason() {
            return reason;
        }
    }

    /**
     * 지원하지 않는 코트로 확인을 요청했다.
     *
     * <p>{@link #findCourt(String)} 로 먼저 걸러낼 수 있는 상황이지만, 걸러내지 않고 확인을 요청했을 때
     * 조용히 빈 결과를 돌려주면 "지원하지 않는 코트"와 "날짜를 열지 않은 코트"가 같은 모양이 된다.
     */
    class CourtNotSupportedException extends Exception {

        private final String courtId;

        public CourtNotSupportedException(String courtId) {
            super("지원하지 않는 코트: " + courtId);
            this.courtId = courtId;
        }

        public String courtId() {
            return courtId;
        }
    }
}
