package com.example.security_log_system.controller;


import com.example.security_log_system.dto.LogResponseDto;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.LogRepository;
import com.example.security_log_system.service.LogService;
import com.example.security_log_system.validation.ValidIpAddress;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 기능 : 전체 로그 조회 API

@Tag(name = "Log API", description = "전체 로그 조회 및 관리 API")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    // GET
    @GetMapping
    public ResponseEntity<Page<LogResponseDto>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC,"createdAt")
        );
        return ResponseEntity.ok(logService.getAllLogs(pageable));
    }

    // 특정 IP로 로그 검색하기
    @GetMapping("/search")
    public ResponseEntity<Page<LogResponseDto>> getLogByIp(
            @RequestParam
            @ValidIpAddress
            String ip,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
            ){
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC,"createdAt")
        );

        return ResponseEntity.ok(logService.getLogByIp(ip,pageable));

    }
}