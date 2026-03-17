package com.example.security_log_system.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// AI서버 -> 백엔드 전송용
@Getter
@NoArgsConstructor
public class ThreatDto {
    private String threatType;  // SQL injection, XSS 등
    private String clientIp;    // 공격자 IP
    private int riskLevel;      // 1~5단계
    private String description; // 상세 설명
}
