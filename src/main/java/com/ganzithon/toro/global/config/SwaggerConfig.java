package com.ganzithon.toro.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui/index.html (오른쪽 위 Authorize에 로그인 응답의 accessToken만 넣으면 됨)
 * 명세 파일: /v3/api-docs → docs/openapi.json 으로 저장해 프론트와 공유한다.
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER = "bearerAuth";

    private static final String DESCRIPTION = """
            ## 공통
            - 모든 응답: `{ "success": boolean, "data": ..., "error": { "code", "message" } | null }`
            - 아래 각 API의 응답 예시는 `data` 안의 모양이다. DELETE와 비밀번호 변경은 204(본문 없음)
            - 토큰: 로그인 응답의 `accessToken`을 `Authorization: Bearer {토큰}`으로 보낸다. 유효 7일
            - 토큰 없이 부르는 API: 회원가입, 로그인, 이메일·닉네임 중복 확인
            - 프론트는 `error.code`로 분기하고 화면 문구는 code별로 정한다

            ## 공통 에러
            | 코드 | HTTP | 뜻 |
            |---|---|---|
            | COMMON_001 | 400 | 입력값 형식 오류. message 앞에 필드 이름이 붙음 (예: `nickname: 한글·영문·숫자 1~10자여야 합니다`) |
            | COMMON_002 | 401 | 토큰이 없거나 만료, 탈퇴한 계정 → 토큰 지우고 로그인 화면으로 |
            | COMMON_003 | 500 | 서버 오류 |
            """;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("토로(TORO) API").version("v1").description(DESCRIPTION))
                // 명세 파일에 실행한 포트가 박히지 않게 고정한다. "/"는 Swagger UI를 연 서버 그대로
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("로컬 기본 주소"),
                        new Server().url("/").description("Swagger UI를 연 서버")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
