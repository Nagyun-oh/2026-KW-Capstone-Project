package com.example.security_log_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// 일반 Web MVC 설정, CORS, RestTemplate, Async/Scheduling 활성화 클래스

@Configuration      // Spring 설정 클래스
@EnableAsync        // 비동기 메서드 실행을 활성화함. ex) @Async가 붙은 메서드를 별도 스레드에서 실행할 수 있게 해준다.
@EnableScheduling   // 스케줄러 기능을 활성화함. ex) @Scheduled가 붙은 메서드를 주기적으로 실행할 수 있게 해준다.
public class WebConfig implements WebMvcConfigurer {

    // Spring MVC 레벨에서 CORS를 설정하는 메서드
    // 모든 API 경로에 대해 React 개발 서버의 요청을 허용한다.
    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    // RestTemplate은 Spring에서 외부 HTTP API를 호출할 때 사용한다.
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
