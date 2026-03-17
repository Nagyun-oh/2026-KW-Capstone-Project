package com.example.security_log_system.controller;


import com.example.security_log_system.dto.ThreatDto;
import com.example.security_log_system.entity.DetectedThreat;
import com.example.security_log_system.repository.ThreatRepository;
import com.example.security_log_system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/threats")
@RequiredArgsConstructor
public class ThreatController {

    private final LogService logService;
    private final ThreatRepository threatRepository;

    // 현재 탐지된 모든 목록 조회
    @GetMapping
    public ResponseEntity<List<DetectedThreat>> getAllThreats()
    {
        return ResponseEntity.ok(threatRepository.findAll());
    }


    // AI 팀원에게 보여줄 "너가 여기로 데이터 던지면 돼" 라는 주소
    @PostMapping("/detect")
    public ResponseEntity<String> receiveDetect(@RequestBody ThreatDto threatDto){
        logService.saveDetectedThreat(threatDto);
        return ResponseEntity.ok("Threat recorded successfully");
    }


}
