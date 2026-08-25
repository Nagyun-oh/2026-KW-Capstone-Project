package com.example.security_log_system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// application.yml의 서비스별 정적 리소스 제외 조건을 바인딩한다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.log-filter.static-resource")
public class StaticResourceFilterProperties {

    // 기본값은 비활성화하여 설정 누락 시 원본 로그를 보존한다.
    private boolean enabled = false;
    // 운영 대상 서비스에서 정적 파일 전용으로 사용하는 경로만 등록한다.
    private List<String> pathPrefixes = new ArrayList<>();
    // 클라이언트 요청값이 아닌 서버 응답 Content-Type과 비교한다.
    private List<String> contentTypes = new ArrayList<>();
    // Content-Type이 생략될 수 있는 304 응답에서만 보조 조건으로 사용한다.
    private List<String> extensions = new ArrayList<>();
    private List<String> methods = new ArrayList<>(List.of("GET", "HEAD"));
}
