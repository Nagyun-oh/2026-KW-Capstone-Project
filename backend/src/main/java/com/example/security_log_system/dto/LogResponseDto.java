package com.example.security_log_system.dto;

import com.example.security_log_system.entity.LogEntry;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class LogResponseDto {

    private Long id;
    private String ipAddress;
    private String requestMethod;
    private String requestUrl;
    private int statusCode;
    private String rawLog;
    private LocalDateTime createdAt;

    public static LogResponseDto from(LogEntry logEntry){
        return LogResponseDto.builder()
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
