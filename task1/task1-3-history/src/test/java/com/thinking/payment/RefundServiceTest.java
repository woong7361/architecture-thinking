package com.thinking.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private PgClient pg;

    @Test
    @DisplayName("PG 취소 성공이면 환불은 성공, 주문은 환불 완료 상태가 된다")
    void pg_취소_성공이면_환불과_주문을_성공_상태로_전이한다() {
        Order order = Order.paid("payment-uuid-1", 30000);
        Refund refund = Refund.proration(30, 7);
        RefundService service = new RefundService(pg);

        // PG는 비관리형 외부 시스템이라 이 경계에서만 Mock으로 격리한다.
        when(pg.cancelPayment("payment-uuid-1", 30000))
            .thenReturn(PgCancelResult.succeeded());

        service.cancel(order, refund);

        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(order.status()).isEqualTo(OrderStatus.REFUNDED);
        verify(pg).cancelPayment("payment-uuid-1", 30000);
    }
}
