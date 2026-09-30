package com.thinking.tennis.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * 유스케이스가 시간을 직접 읽지 않게 시계를 빈으로 둔다.
 *
 * <p>타임스탬프는 UTC로 표기하므로 시계도 UTC다. 코트가 있는 지역의 날짜와 시각은 저장하고 표시하는
 * 값이고, 시점을 재는 기준은 하나여야 한다.
 *
 * <p>만료를 옮기는 작업이 주기로 도므로 예약 실행을 켠다.
 */
@Configuration
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
