package com.thinking.ticket.support;

import com.thinking.ticket.core.application.TicketService;
import com.thinking.ticket.core.domain.DiscountPolicy;
import com.thinking.ticket.core.port.out.ChargePort;
import com.thinking.ticket.core.port.out.LoadTicketPort;
import com.thinking.ticket.core.port.out.LoadUserPort;
import com.thinking.ticket.core.port.out.SaveTicketPort;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 조립 슬롯의 대역. 조립부가 아직 실물이 아닐 때, 아웃바운드 어댑터를 포트 자리에 꽂아
 * 유스케이스를 만들어 준다 — 조립 층이 없어도 실제 저장소 구성을 실행할 수 있게 하는 장치다.
 *
 * <p>기본은 꺼져 있고 {@code -Dskeleton.composition-root=stub} 일 때만 켜진다.
 * 조립 층이 실물로 승격되면 이 대역을 끄고 같은 심판을 다시 돌린다. 그 대비가 그 층의 판정이다.
 *
 * <p>심판 자산이므로 파이프라인은 이 파일을 읽지도 고치지도 못한다.
 */
@TestConfiguration
@ConditionalOnProperty(name = "skeleton.composition-root", havingValue = "stub")
public class StubCompositionRoot {

    @Bean
    public DiscountPolicy discountPolicy() {
        return new DiscountPolicy();
    }

    @Bean
    public TicketService ticketService(LoadTicketPort loadTicketPort,
                                       SaveTicketPort saveTicketPort,
                                       LoadUserPort loadUserPort,
                                       ChargePort chargePort,
                                       DiscountPolicy discountPolicy) {
        return new TicketService(loadTicketPort, saveTicketPort, loadUserPort, chargePort, discountPolicy);
    }
}
