package com.thinking.ticket.adapter.out.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/* walking skeleton 데모용 시드 데이터. compose 기동 직후 REST 데모가 동작하게 하는 최소 시드다
 * (실서비스 데이터 마이그레이션이 아니다).
 *
 * <p>멱등 + 경합 안전: 각 행을 id 존재 여부로 건너뛰고, 그래도 동시 기동(--scale) 중
 * 다른 인스턴스가 먼저 넣어 중복키가 나면 무시한다 — 진 인스턴스가 기동 실패하지 않도록. */
@Component
public class DataSeeder implements ApplicationRunner {

    private final TicketJpaRepository ticketRepo;
    private final UserJpaRepository userRepo;

    public DataSeeder(TicketJpaRepository ticketRepo, UserJpaRepository userRepo) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser(new UserJpaEntity(1, "user-1"));

        seedTicket(new TicketJpaEntity(20, 30_000, false, false, 0)); // 예약 가능
        seedTicket(new TicketJpaEntity(21, 30_000, false, true, 0));  // 판매 중지
        seedTicket(new TicketJpaEntity(22, 30_000, true, false, 1));  // 이미 예약됨
        seedTicket(new TicketJpaEntity(23, 50_000, false, false, 0)); // 할인 적용 대상(>=5만)
    }

    private void seedUser(UserJpaEntity user) {
        if (userRepo.existsById(user.getId())) {
            return;
        }
        try {
            userRepo.save(user);
        } catch (DataIntegrityViolationException alreadySeededByAnotherInstance) {
            // 무시 — 다른 인스턴스가 먼저 시드함
        }
    }

    private void seedTicket(TicketJpaEntity ticket) {
        if (ticketRepo.existsById(ticket.getId())) {
            return;
        }
        try {
            ticketRepo.save(ticket);
        } catch (DataIntegrityViolationException alreadySeededByAnotherInstance) {
            // 무시 — 다른 인스턴스가 먼저 시드함
        }
    }
}
