package com.example.security_log_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger/OpenAPI 문서에서 JWT 인증을 사용할 수 있게 설정하는 클래스

@Configuration
public class SwaggerConfig {

    // SpringDoc OpenAPI 설정 객체를 Bean으로 등록한다.
    @Bean
    public OpenAPI openAPI(){

        // Swagger UI에서 사용할 인증 방식 이름
        String securitySchemeName = "bearerAuth";

        // Swagger UI에 JWT Bearer Token 입력 기능을 추가하는 설정
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                );
    }
}
