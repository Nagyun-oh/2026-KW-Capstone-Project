package com.example.security_log_system.dto;

import com.example.security_log_system.entity.LogEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
// 사용자가 로그를 선택했을 때 원본 rawLog까지 제공하는 상세 응답이다.
public class LogDetailResponseDto {

    private Long id;
    private String ipAddress;
    private String requestMethod;
    private String requestUrl;
    private int statusCode;
    private String rawLog;
    private LocalDateTime createdAt;

    public static LogDetailResponseDto from(LogEntry logEntry) {
        return LogDetailResponseDto.builder()
                .id(logEntry.getId())
                .ipAddress(logEntry.getIpAddress())
                .requestMethod(logEntry.getRequestMethod())
                .requestUrl(logEntry.getRequestUrl())
                .statusCode(logEntry.getStatusCode())
                .rawLog(logEntry.getRawLog())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
