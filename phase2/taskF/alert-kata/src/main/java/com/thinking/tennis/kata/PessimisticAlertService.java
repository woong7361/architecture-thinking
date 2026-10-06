package com.thinking.tennis.kata;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PessimisticAlertService {
    private final AlertRepository repository;

    public PessimisticAlertService(AlertRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public long subscribe(SubscribeCommand command) {
        repository.lockUserForUpdate(command.userId());
        return repository.findActive(command)
                .orElseGet(() -> repository.save(new AlertSubscription(command)))
                .id();
    }
}
