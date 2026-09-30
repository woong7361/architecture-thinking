package com.thinking.tennis.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * 유스케이스가 쓰는 바깥 값을 엮는다.
 *
 * <p>시계를 빈으로 두는 것은 만료와 확인 간격이 시간에 걸린 판단이기 때문이다. {@code Instant.now()} 를
 * 코드 안에서 직접 부르면 그 판단을 시험할 때 실제 시간을 기다리는 수밖에 없다.
 *
 * <p>만료는 엔드포인트가 아니라 서버 작업이 옮기므로 스케줄링을 켠다. 부팅 진입점은 사람이 소유해
 * 고칠 수 없으니 이 자리에서 켠다.
 */
@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
