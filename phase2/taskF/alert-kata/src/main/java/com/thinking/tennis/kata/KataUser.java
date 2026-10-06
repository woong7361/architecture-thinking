package com.thinking.tennis.kata;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "kata_users")
public class KataUser {
    @Id
    private Long id;

    // F-3에서 이미 존재하는 사용자 행을 대상으로 충돌을 감지할 수 있게 준비한다.
    @Version
    private long version;

    protected KataUser() {
    }
}
