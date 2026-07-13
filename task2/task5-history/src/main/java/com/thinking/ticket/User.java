package com.thinking.ticket;

/* 빈약한 도메인 모델(Anemic): 데이터 운반 그릇. */
public class User {

    private long id;
    private String name;

    public User(long id, String name) {
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
