package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort;

/**
 * 사람에게 보이는 코트의 신원이다. 이름과 예약 화면 주소 둘이다.
 *
 * <p>이 값을 읽는 규칙을 한자리에 둔다. 예약 상태 조회와 신청 조회가 같은 코트를 다른 시점의 이름과
 * 링크로 말하면 어느 쪽이 참인지 클라이언트가 고를 근거가 없고, 낡은 링크를 눌러 예약을 시도하지 못하면
 * 이 서비스가 존재하는 이유가 사라진다. 그래서 두 응답을 만드는 자리가 모두 여기를 지난다.
 *
 * <p>규칙은 하나다. 읽는 시점에 포트에서 읽고, 코트가 더 이상 지원되지 않아 읽을 값이 없으면 보관된
 * 값으로 물러난다. 계약이 두 필드를 필수로 두었으므로 비울 수는 없다.
 */
public record CourtIdentity(String courtName, String reservationUrl) {

    /** 방금 포트에서 읽은 코트다. 다시 읽지 않는다. */
    public static CourtIdentity of(CourtAvailabilityPort.SupportedCourt court) {
        return new CourtIdentity(court.courtName(), court.reservationUrl());
    }

    /** 지금 그 코트의 신원이다. 지원이 끊겨 읽을 값이 없으면 보관된 값으로 물러난다. */
    public static CourtIdentity read(CourtAvailabilityPort courts,
                                     String courtId,
                                     String storedName,
                                     String storedReservationUrl) {
        return courts.findCourt(courtId)
                .map(CourtIdentity::of)
                .orElseGet(() -> new CourtIdentity(storedName, storedReservationUrl));
    }
}
