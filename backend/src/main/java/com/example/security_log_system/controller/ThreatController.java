package com.example.security_log_system.controller;

import com.example.security_log_system.dto.ThreatDto;
import com.example.security_log_system.dto.ThreatResponseDto;
import com.example.security_log_system.dto.ThreatSearchCondition;
import com.example.security_log_system.service.ThreatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Threat API", description = "위협 데이터 조회 및 관리 API")
@RestController
@RequestMapping("/api/v1/threats")
@RequiredArgsConstructor
public class ThreatController {


    private final ThreatService threatService;

    // 전체 위협 조회
    @GetMapping
    public ResponseEntity<Page<ThreatResponseDto>> getThreats(
            @Valid @ModelAttribute ThreatSearchCondition condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "detectedAt")
        );

        return ResponseEntity.ok(threatService.getThreats(condition,pageable));
    }


    // POST
    @PostMapping("/detect")
    public ResponseEntity<String> receiveDetect(@Valid @RequestBody ThreatDto threatDto){
        threatService.saveDetectedThreat(threatDto);
        return ResponseEntity.ok("Threat recorded successfully.");
    }
}
