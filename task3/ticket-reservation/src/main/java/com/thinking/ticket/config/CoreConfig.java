package com.thinking.ticket.config;

import com.thinking.ticket.core.application.TicketService;
import com.thinking.ticket.core.domain.DiscountPolicy;
import com.thinking.ticket.core.port.out.ChargePort;
import com.thinking.ticket.core.port.out.TicketRepository;
import com.thinking.ticket.core.port.out.UserRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/* 조립 지점(Composition Root). Core는 스스로를 Spring 빈으로 등록하지 않는다(프레임워크 무의존) —
 * 대신 여기서 Outbound Adapter들을 Port 자리에 주입해 TicketService를 만든다.
 * TicketService는 ReserveTicketUseCase를 구현하므로 Controller가 이 빈을 Inbound Port로 주입받는다. */
@Configuration
public class CoreConfig {

    @Bean
    public DiscountPolicy discountPolicy() {
        return new DiscountPolicy();
    }

    @Bean
    public TicketService ticketService(TicketRepository ticketRepository,
                                       UserRepository userRepository,
                                       ChargePort chargePort,
                                       DiscountPolicy discountPolicy) {
        return new TicketService(ticketRepository, userRepository, chargePort, discountPolicy);
    }
}
