package com.thinking.tennis.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * 계약의 문서 수준 선언을 코드에 둔다.
 *
 * <p>보안 요구와 서버 주소와 태그를 도구의 기본값에 맡기지 않고 여기서 선언한다. 추출한 스펙이 계약과
 * 같은 말을 해야 하고, 기본값에 맡기면 무엇이 약속이고 무엇이 도구의 습관인지 구별되지 않는다.
 *
 * <p>보안 요구는 문서 수준에서 Bearer 토큰이다. 예약 상태 조회만 그 요구를 비운다.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "테니스 코트 빈자리 알림 API",
                version = "1.0.0",
                description = """
                        사용자가 지정한 코트·날짜·시간대에 빈자리가 생기면 알려 주는 서비스의 HTTP 계약이다.
                        요구사항은 `phase2/taskE/tennis-court-service-plan.md`이며, 이 파일의 모든 요소는 그 문서의 항목에서 왔다.
                        각 오퍼레이션의 `x-requirement`가 출처를 가리킨다.

                        ## 계약 범위 밖

                        - 예약과 취소는 외부 예약 사이트에서 이뤄진다. 이 API로는 예약할 수 없고 예약 성공 여부도 알 수 없다.
                        - 알림 발송(FR-4)과 만료(FR-5)는 서버가 스스로 수행하므로 엔드포인트가 없다. 그 결과는 신청 조회 응답의 `status`와 `delivery`로 드러난다.
                        - 코트 목록과 검색은 이 서비스의 범위 밖이다. 예약 상태는 코트 하나와 날짜 하나 단위로만 가져오고 조회하며, `courtId`는 밖에서 받는다.
                        - 인증 방식 자체는 이 계약이 정하지 않는다. 남의 신청을 조회·해제할 수 없다는 FR-2의 요구 때문에 사용자 식별이 필요하며, 그 수단으로 Bearer 토큰을 쓴다.
                        - 빈자리를 확인한 뒤 알림이 단말까지 가는 경로(FR-4, FR-6)는 요청과 응답이 아니라 채널을 지나는 메시지라 `notification-asyncapi.yaml`이 정한다. 두 계약은 같은 개념에 같은 이름과 형식을 쓰며, 그 일치는 계약 검사 스크립트가 판정한다.

                        ## 시간 표현

                        - 타임스탬프는 RFC 3339 형식이며 UTC로 표기한다.
                        - `date`는 코트가 있는 지역의 날짜이고, `startTime`과 `endTime`은 그 지역의 24시간제 시각이다. 현재 지원 지역의 시간대는 `Asia/Seoul`이다.

                        ## 호환성

                        - 경로의 `/v1`이 주 버전이다. 호환을 깨는 변경은 주 버전을 올려야 하며 같은 버전 안에서는 할 수 없다.
                        - 호환을 깨는 변경은 넷이다. 요청에 필수 필드를 더하는 것, 응답에서 필드를 빼는 것, 열거형에서 값을 빼는 것, 타입이나 형식을 좁히는 것이다.
                        - 응답의 열거형에 값을 더하는 것은 호환을 깨지 않는다. 클라이언트는 모르는 값을 받으면 무시하지 말고 알 수 없는 상태로 다뤄야 한다.
                        """,
                extensions = @Extension(properties = @ExtensionProperty(
                        name = "x-out-of-scope",
                        value = """
                                [{"requirement":"NFR-2","reason":"응답시간 목표는 이 계약이 정하는 요청과 응답의 모양이 아니라 운영이 지켜야 할 성능 기준이다. 부하 시험으로 판정한다."}]
                                """,
                        parseValue = true))),
        servers = @Server(url = "https://api.tennis-alert.example.com/v1"),
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제"),
                @Tag(name = "availability", description = "외부 예약처에서 가져온 예약 상태 조회")
        })
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        description = "사용자를 식별한다. 토큰이 가리키는 사용자만 자기 신청을 조회하고 해제할 수 있다.")
public class OpenApiDeclaration {
}
