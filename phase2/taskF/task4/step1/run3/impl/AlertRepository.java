package com.thinking.tennis.kata;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AlertRepository {
    private final EntityManager entityManager;

    public AlertRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 신청자 행을 쓰기 잠금으로 선점한다. 같은 사용자의 신청은 이 지점부터 한 줄로 세워진다.
     * 이미 커밋되어 존재하는 행을 잠그므로 갭 잠금이 생기지 않고, 다른 사용자끼리는 서로 기다리지 않는다.
     */
    public void lockSubscriber(long userId) {
        KataUser subscriber = entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (subscriber == null) {
            throw new IllegalStateException("Unknown subscriber: " + userId);
        }
    }

    public Optional<AlertSubscription> findActive(SubscribeCommand command) {
        return entityManager.createQuery("""
                select a from AlertSubscription a
                where a.userId = :userId and a.courtId = :courtId
                  and a.playDate = :playDate and a.timeSlot = :timeSlot
                  and a.status = :status
                order by a.id
                """, AlertSubscription.class)
                .setParameter("userId", command.userId())
                .setParameter("courtId", command.courtId())
                .setParameter("playDate", command.playDate())
                .setParameter("timeSlot", command.timeSlot())
                .setParameter("status", AlertSubscription.WATCHING)
                .setMaxResults(1)
                .getResultList().stream().findFirst();
    }

    public AlertSubscription save(AlertSubscription subscription) {
        entityManager.persist(subscription);
        // IDENTITY 키라 persist 시점에 INSERT 가 나가고, 유일 제약 위반도 이 자리에서 드러난다.
        entityManager.flush();
        return subscription;
    }
}
