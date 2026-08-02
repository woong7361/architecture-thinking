package com.thinking.ticket.config;

import com.thinking.ticket.core.application.TicketService;
import com.thinking.ticket.core.domain.DiscountPolicy;
import com.thinking.ticket.core.port.in.ReserveTicketUseCase;
import com.thinking.ticket.core.port.out.ChargePort;
import com.thinking.ticket.core.port.out.LoadTicketPort;
import com.thinking.ticket.core.port.out.LoadUserPort;
import com.thinking.ticket.core.port.out.SaveTicketPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/* 조립 지점. Core에는 프레임워크 애노테이션을 붙이지 않기로 했으므로(컨테이너 없이 실행·테스트되어야 한다)
 * 컴포넌트 스캔은 유스케이스를 발견하지 못한다. 그 선택의 대가를 이 한 곳에서 치른다 —
 * 스캔이 찾아낸 아웃바운드 어댑터를 포트 자리에 꽂아 유스케이스를 손으로 만든다.
 *
 * 이 클래스에는 조립만 둔다. 조건 분기나 값 계산이 여기 생기면 업무 규칙이 설정으로 새고,
 * 그 규칙은 어떤 도메인 테스트도 보지 못하는 자리에 놓인다. */
@Configuration
public class CompositionRoot {

    /* 가격 정책도 Core의 순수 객체라 스스로 빈이 되지 않는다. 인스턴스를 만들 책임은 결국 조립부에
     * 남으므로, 유스케이스 안에서 몰래 new 하지 않고 이 자리에 드러내 둔다. */
    @Bean
    public DiscountPolicy discountPolicy() {
        return new DiscountPolicy();
    }

    /* 반환 타입을 구현 클래스가 아니라 Inbound Port로 둔다 — 주입받는 쪽(HTTP 어댑터든 테스트든)이
     * TicketService라는 구체 이름을 알게 되는 순간 교체 축이 사라진다.
     *
     * 파라미터도 포트 타입으로만 받는다. 어떤 어댑터가 꽂히는지는 구성이 정하므로,
     * 저장이 인메모리에서 JPA로 바뀌어도 이 메서드는 그대로다. */
    @Bean
    public ReserveTicketUseCase reserveTicketUseCase(LoadTicketPort loadTicketPort,
                                                     SaveTicketPort saveTicketPort,
                                                     LoadUserPort loadUserPort,
                                                     ChargePort chargePort,
                                                     DiscountPolicy discountPolicy) {
        return new TicketService(loadTicketPort, saveTicketPort, loadUserPort, chargePort, discountPolicy);
    }
}
