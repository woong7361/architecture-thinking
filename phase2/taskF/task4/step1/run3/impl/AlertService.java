package com.thinking.tennis.kata;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
    private static final int MAX_ATTEMPTS = 3;

    private final AlertRegistrar registrar;

    public AlertService(AlertRegistrar registrar) {
        this.registrar = registrar;
    }

    /**
     * 재시도는 트랜잭션 밖에서 돌린다. 시도들이 한 트랜잭션에 함께 들어가면
     * 실패한 시도의 롤백이 다음 시도까지 끌고 가고, 재조회도 같은 스냅샷을 다시 볼 뿐이다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public long subscribe(SubscribeCommand command) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return registrar.register(command);
            } catch (DataIntegrityViolationException | ConcurrencyFailureException conflict) {
                // 유일 제약 위반, 교착, 잠금 대기 초과는 경쟁의 결과이므로 새 트랜잭션에서 다시 판단한다.
                lastFailure = conflict;
            }
        }
        throw lastFailure;
    }
}
