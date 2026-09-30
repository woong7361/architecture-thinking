package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.TimeSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 신청을 보관하는 포트다.
 *
 * <p>저장은 인메모리다. 계약을 지켰는지 판정하는 데 실제 저장소가 필요하지 않고, 게이트가 iteration마다
 * 부팅하므로 부팅이 빨라야 한다.
 */
public interface AlertRepository {

    /**
     * 같은 사용자가 같은 조건으로 감시 중인 신청이 없으면 새 신청을 저장한다.
     *
     * <p>있으면 저장하지 않고 그 신청을 돌려준다. 같은 조건으로 동시에 두 번 신청해도 신청이 하나여야
     * 해서(V-1) 조회와 저장을 한 호출로 묶는다. 두 단계로 나누면 그 사이에 다른 요청이 끼어든다.
     *
     * @return 이미 감시 중이던 신청, 없었으면 저장한 신청
     */
    Alert insertIfNotWatching(Alert candidate);

    Optional<Alert> findById(String alertId);

    /**
     * 사용자 본인의 신청을 최근에 신청한 것부터 돌려준다.
     *
     * @param statusFilter 이 상태로 저장된 신청만 돌려준다. null이면 끝난 신청까지 모두 돌려준다
     */
    List<Alert> findByUserNewestFirst(String userId, AlertStatus statusFilter);

    Optional<Alert> findWatching(String userId, String courtId, LocalDate date, TimeSlot slot);

    /**
     * 그 코트·날짜를 감시 중인 신청 전부다. 사용자를 가리지 않는다.
     *
     * <p>확인 결과 하나가 그 코트·날짜를 보는 신청 모두에게 같은 사실을 말하므로, 확인이 끝난 자리가
     * 발송 대기를 올리고 버릴 때 이 목록을 쓴다.
     */
    List<Alert> findWatchingByCourtDay(String courtId, LocalDate date);

    /** 감시 중인 신청 전부다. 만료 작업이 훑는다. */
    List<Alert> findAllWatching();

    /**
     * 저장된 신청이 {@code expected} 와 같을 때만 {@code updated} 로 바꾼다.
     *
     * <p>읽고 고치는 사이에 다른 요청이 같은 신청을 고쳤으면 거짓을 돌려준다. 그 경우를 조용히 덮어쓰면
     * 나중에 온 해제가 먼저 온 해제를 지운다.
     *
     * @return 바꿨으면 true, 경합에 밀렸으면 false
     */
    boolean compareAndSet(Alert expected, Alert updated);
}
