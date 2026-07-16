package com.thinking.ticket.provided;

/**
 * 회원 저장소가 다루는 영속 레코드. 기존 인프라이며 수정할 수 없다.
 */
public final class UserRecord {

    private final long id;
    private final String name;

    public UserRecord(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
