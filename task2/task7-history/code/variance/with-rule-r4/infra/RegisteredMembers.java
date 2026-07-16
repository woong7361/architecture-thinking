package com.thinking.ticket.infra;

import com.thinking.ticket.Member;
import com.thinking.ticket.Members;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserStore;

import java.util.Optional;

/**
 * 사내 저장소들을 회원 명부 역할에 맞춘다.
 *
 * <p>"없으면 null"이라는 저장소의 관례를 여기서 번역해, 정책이 null을 다루지 않게 한다.
 * 없는 회원을 거부할지 말지는 번역하지 않는다 — 그건 정책의 판단이다.
 *
 * <p>회원이 몇 장을 가졌는지는 회원의 사실이지만 티켓 저장소에 적혀 있다. 그 사실이 어디에
 * 적혀 있는지를 가리는 것이 이 클래스의 일이므로, 두 저장소를 함께 읽어 회원 하나를 만들어 준다.
 * 바뀔 이유는 하나다: 회원의 사실이 어디에 어떻게 담기는지가 바뀔 때.
 */
public final class RegisteredMembers implements Members {

    private final UserStore users;
    private final TicketStore tickets;

    public RegisteredMembers(UserStore users, TicketStore tickets) {
        this.users = users;
        this.tickets = tickets;
    }

    @Override
    public Optional<Member> byId(long userId) {
        if (users.findById(userId) == null) {
            return Optional.empty();
        }
        return Optional.of(new Member(userId, tickets.countByUserId(userId)));
    }
}
