package com.example.security_log_system.controller;


import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.LogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 전체 로그 조회용
// 카프카를 통해 들어온 모든 원본 로그 확인하는 곳
@Tag(name = "Log API", description = "전체 로그 조회 및 관리 API")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogRepository logRepository;

    //전체 로그 리스트 조회(최신순 100개 등으로 제한가능)
    @GetMapping
    public ResponseEntity<List<LogEntry>> getAllLogs(){
        return ResponseEntity.ok(logRepository.findAll());
    }

    // 특정 IP로 로그 검색하기
    @GetMapping("/search")
    public ResponseEntity<List<LogEntry>> getLogByIp(@RequestParam String ip){
        return ResponseEntity.ok(logRepository.findByIpAddress(ip));
    }

}
