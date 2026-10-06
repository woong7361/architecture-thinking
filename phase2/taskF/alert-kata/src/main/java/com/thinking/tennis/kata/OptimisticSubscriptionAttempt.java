package com.thinking.tennis.kata;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class OptimisticSubscriptionAttempt {
    private final AlertRepository repository;

    OptimisticSubscriptionAttempt(AlertRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    long subscribe(SubscribeCommand command) {
        long version = repository.currentUserVersion(command.userId());
        if (!repository.advanceUserVersion(command.userId(), version)) {
            throw new OptimisticConflictException(command.userId());
        }
        return repository.findActive(command)
                .orElseGet(() -> repository.save(new AlertSubscription(command)))
                .id();
    }
}
