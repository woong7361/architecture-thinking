package com.thinking.tennis.api;

import java.util.Map;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "테니스 코트 빈자리 알림 API", version = "1.0.0"),
        servers = @Server(url = "https://api.tennis-alert.example.com/v1"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class OpenApiConfig {

    private static final Map<String, Map<String, String[]>> ERROR_CODES = Map.of(
            "getCourtAvailability", Map.of(
                    "400", new String[]{"VALIDATION_FAILED"},
                    "404", new String[]{"COURT_NOT_SUPPORTED"},
                    "500", new String[]{"INTERNAL_ERROR"},
                    "502", new String[]{"UPSTREAM_RESPONSE_UNREADABLE"},
                    "503", new String[]{"UPSTREAM_UNAVAILABLE", "STORAGE_TIMEOUT"},
                    "504", new String[]{"UPSTREAM_TIMEOUT"}),
            "createAlert", Map.of(
                    "400", new String[]{"VALIDATION_FAILED"},
                    "401", new String[]{"UNAUTHENTICATED"},
                    "409", new String[]{"IDEMPOTENCY_KEY_REUSED"},
                    "422", new String[]{"COURT_NOT_SUPPORTED", "SLOT_NOT_SUPPORTED", "ALERT_WINDOW_CLOSED"},
                    "500", new String[]{"INTERNAL_ERROR"},
                    "502", new String[]{"UPSTREAM_RESPONSE_UNREADABLE"},
                    "503", new String[]{"UPSTREAM_UNAVAILABLE", "STORAGE_TIMEOUT", "CONCURRENT_UPDATE_CONFLICT"},
                    "504", new String[]{"UPSTREAM_TIMEOUT"}),
            "listAlerts", Map.of(
                    "400", new String[]{"VALIDATION_FAILED"},
                    "401", new String[]{"UNAUTHENTICATED"},
                    "500", new String[]{"INTERNAL_ERROR"},
                    "503", new String[]{"STORAGE_TIMEOUT"}),
            "getAlert", Map.of(
                    "400", new String[]{"VALIDATION_FAILED"},
                    "401", new String[]{"UNAUTHENTICATED"},
                    "404", new String[]{"ALERT_NOT_FOUND"},
                    "500", new String[]{"INTERNAL_ERROR"},
                    "503", new String[]{"STORAGE_TIMEOUT"}),
            "cancelAlert", Map.of(
                    "400", new String[]{"VALIDATION_FAILED"},
                    "401", new String[]{"UNAUTHENTICATED"},
                    "404", new String[]{"ALERT_NOT_FOUND"},
                    "500", new String[]{"INTERNAL_ERROR"},
                    "503", new String[]{"STORAGE_TIMEOUT", "CONCURRENT_UPDATE_CONFLICT"}));

    @Bean
    OpenApiCustomizer errorCodeExtensions() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    Map<String, String[]> codes = ERROR_CODES.get(operation.getOperationId());
                    if (codes == null || operation.getResponses() == null) {
                        return;
                    }
                    codes.forEach((status, values) -> {
                        if (operation.getResponses().get(status) != null) {
                            operation.getResponses().get(status).addExtension("x-error-codes", values);
                        }
                    });
                }));
    }
}
