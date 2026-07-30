package com.thinking.ticket.support;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/* 실제 저장소를 쓰는 구성들이 공유하는 MySQL 컨테이너.
 * 구성마다 따로 띄우면 같은 JVM에서 컨테이너가 여러 개 뜨므로, 한 번만 띄워 재사용한다.
 * (@Testcontainers JUnit 확장 없이 Cucumber에서 안전하게 쓰려고 정적 초기화로 기동한다.) */
public final class SharedMySql {

    public static final MySQLContainer<?> INSTANCE = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));

    static {
        INSTANCE.start();
    }

    private SharedMySql() {
    }
}
