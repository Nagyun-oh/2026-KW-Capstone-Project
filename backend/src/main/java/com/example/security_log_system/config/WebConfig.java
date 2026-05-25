package com.example.security_log_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// REST API 호출용으로 GET, POST 등 특정 메서드만 허용하고 주소도 제한합니다.
// 리액트(3000번)에서 스프링(8080번)으로 데이터를 요청해도 CORS 에러없이 데이터 허용.

@Configuration
@EnableAsync
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {

    // 1. CORS 설정 (들어오는 길)
    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000") // 리액트 주소허용
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    // 2. RestTemplate 등록 (나가는 길) : for 외부(ai서버,슬랙,이메일.. 등) 통신
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
