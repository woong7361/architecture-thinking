package com.thinking.tennis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 부팅 진입점이다.
 *
 * <p>이 클래스는 사람이 소유한다. 생성 코드는 이 파일을 고치지 않는다.
 * 컴포넌트 스캔 기준이 {@code com.thinking.tennis} 이므로 생성 코드는 그 하위 패키지에 놓이면
 * 따로 등록하지 않아도 스캔된다.
 */
@SpringBootApplication
public class TennisAlertApplication {

    public static void main(String[] args) {
        SpringApplication.run(TennisAlertApplication.class, args);
    }
}
