package com.thinking.tennis.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 구현이 무엇을 약속했는지를 스펙에 명시적으로 적는 자리다.
 *
 * <p>{@code @PropertySource} 로 OpenAPI 3.1을 켠다. 계약이 3.1로 적혀 있는데 추출한 스펙이 3.0이면
 * 널 허용을 적는 방식부터 달라진다. 부팅 설정 파일은 사람이 소유해 고칠 수 없으므로 이 자리에서 켠다.
 *
 * <p>보안 요구와 닫힌 객체를 애노테이션이 아니라 이 자리에서 손보는 것은 두 가지가 애노테이션으로는
 * 드러나지 않기 때문이다. 빈 보안 요구는 "요구가 없다"와 "적지 않았다"를 가릴 수 없어 스펙에 남지 않고,
 * 도구는 선언하지 않은 속성을 허용하는 쪽으로 기본값을 둔다. 계약은 둘을 분명히 적었으므로 구현도
 * 분명히 적는다.
 */
@Configuration
@PropertySource("classpath:openapi-version.properties")
public class OpenApiConfig {

    /** 이 오퍼레이션만 인증을 요구하지 않는다. */
    private static final Set<String> PUBLIC_OPERATION_IDS = Set.of("getCourtAvailability");

    /**
     * 선언하지 않은 속성을 받지 않는 객체 스키마다. 계약이 이 스키마마다
     * {@code additionalProperties: false} 를 적었다.
     */
    private static final List<String> CLOSED_SCHEMAS = List.of(
            "TimeSlot", "AvailabilitySlot", "CourtAvailability", "AlertRequest",
            "AlertDelivery", "Alert", "AlertList", "Problem");

    @Bean
    public OpenAPI tennisAlertOpenApi() {
        Info info = new Info()
                .title("테니스 코트 빈자리 알림 API")
                .version("1.0.0");
        info.addExtension("x-out-of-scope", outOfScope());

        return new OpenAPI()
                .info(info)
                .servers(List.of(new Server().url("https://api.tennis-alert.example.com/v1")))
                .tags(List.of(
                        new Tag().name("alerts").description("빈자리 알림 신청의 생성, 조회, 해제"),
                        new Tag().name("availability").description("외부 예약처에서 가져온 예약 상태 조회")))
                .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth()))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer contractSurfaceCustomizer() {
        return openApi -> {
            dropSecurityFromPublicOperations(openApi);
            closeDeclaredObjects(openApi);
        };
    }

    private static SecurityScheme bearerAuth() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .description("사용자를 식별한다. 토큰이 가리키는 사용자만 자기 신청을 조회하고 해제할 수 있다.");
    }

    /**
     * 요구사항 두 계약 어디에도 표면이 없는 것이다. 응답시간 목표는 요청과 응답의 모양이 아니라 운영이
     * 지켜야 할 성능 기준이라 이 계약이 다루지 않는다.
     */
    private static List<Map<String, Object>> outOfScope() {
        Map<String, Object> responseTime = new LinkedHashMap<>();
        responseTime.put("requirement", "NFR-2");
        responseTime.put("reason", "응답시간 목표는 이 계약이 정하는 요청과 응답의 모양이 아니라 "
                + "운영이 지켜야 할 성능 기준이다. 부하 시험으로 판정한다.");
        return List.of(responseTime);
    }

    private static void dropSecurityFromPublicOperations(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        for (PathItem item : openApi.getPaths().values()) {
            item.readOperations().stream()
                    .filter(operation -> PUBLIC_OPERATION_IDS.contains(operation.getOperationId()))
                    .forEach(operation -> operation.setSecurity(new ArrayList<>()));
        }
    }

    private static void closeDeclaredObjects(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        for (String name : CLOSED_SCHEMAS) {
            Schema schema = schemas.get(name);
            if (schema != null) {
                schema.setAdditionalProperties(Boolean.FALSE);
            }
        }
    }
}
