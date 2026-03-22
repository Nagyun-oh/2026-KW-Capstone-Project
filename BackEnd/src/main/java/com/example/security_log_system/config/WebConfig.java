package com.example.security_log_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 리액트(3000번)에서 스프링(8080번)으로 데이터를 요청해도 CORS 에러없이 데이터 허용.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000") // 리액트 주소허용
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
