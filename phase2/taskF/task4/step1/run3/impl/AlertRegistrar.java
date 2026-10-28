package com.thinking.tennis.kata;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신청 한 번의 시도를 독립된 트랜잭션으로 처리한다.
 * 시도마다 트랜잭션이 새로 열려야 재조회가 직전에 커밋된 신청을 볼 수 있다.
 */
@Component
public class AlertRegistrar {
    private final AlertRepository repository;

    public AlertRegistrar(AlertRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long register(SubscribeCommand command) {
        repository.lockSubscriber(command.userId());
        return repository.findActive(command)
                .orElseGet(() -> repository.save(new AlertSubscription(command)))
                .id();
    }
}
