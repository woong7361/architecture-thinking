package com.thinking.tennis.kata;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
    private final AlertRepository repository;

    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public long subscribe(SubscribeCommand command) {
        return repository.findActive(command)
                .orElseGet(() -> repository.save(new AlertSubscription(command)))
                .id();
    }
}
