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

    public void lockUserForUpdate(long userId) {
        entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE);
    }

    public long currentUserVersion(long userId) {
        Number version = (Number) entityManager.createNativeQuery("SELECT version FROM kata_users WHERE id = ?")
                .setParameter(1, userId)
                .getSingleResult();
        return version.longValue();
    }

    public boolean advanceUserVersion(long userId, long expectedVersion) {
        int updated = entityManager.createNativeQuery("""
                UPDATE kata_users
                SET version = version + 1
                WHERE id = ? AND version = ?
                """)
                .setParameter(1, userId)
                .setParameter(2, expectedVersion)
                .executeUpdate();
        return updated == 1;
    }

    public AlertSubscription save(AlertSubscription subscription) {
        entityManager.persist(subscription);
        return subscription;
    }
}
