package com.thinking.tennis.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "테니스 코트 빈자리 알림 API", version = "1.0.0"),
        servers = @Server(url = "https://api.tennis-alert.example.com/v1"),
        tags = {
                @Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제"),
                @Tag(name = "availability", description = "외부 예약처에서 가져온 예약 상태 조회")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        description = "사용자를 식별한다. 토큰이 가리키는 사용자만 자기 신청을 조회하고 해제할 수 있다."
)
public class OpenApiConfiguration {
}
