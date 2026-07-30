package com.thinking.ticket.jpa;

import com.thinking.ticket.core.port.out.ChargePort;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/* 결제 아웃바운드 어댑터를 통제 가능한 테스트 더블로 대체한다(외부 PG/HTTP 없이 결정론적).
 * 실제 PaymentHttpAdapter 대신 @Primary 로 이 빈이 ChargePort 자리에 주입된다 — 어댑터 교체의 또 다른 예. */
@TestConfiguration
public class TestPaymentConfig {

    @Bean
    @Primary
    public TestChargePort testChargePort() {
        return new TestChargePort();
    }

    /* 성공/거절을 통제하고 청구를 기록하는 결제 더블(상태로 단언). */
    public static class TestChargePort implements ChargePort {

        private boolean declining = false;
        private int chargeCount = 0;
        private int lastAmount = 0;

        public void decline() {
            this.declining = true;
        }

        public void reset() {
            this.declining = false;
            this.chargeCount = 0;
            this.lastAmount = 0;
        }

        @Override
        public boolean charge(String paymentInfo, int amount) {
            chargeCount++;
            lastAmount = amount;
            return !declining;
        }

        public boolean wasCharged() {
            return chargeCount > 0;
        }

        public int lastAmount() {
            return lastAmount;
        }
    }
}
