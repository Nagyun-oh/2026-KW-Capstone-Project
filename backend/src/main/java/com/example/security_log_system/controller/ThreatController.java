package com.example.security_log_system.controller;

import com.example.security_log_system.dto.ThreatDto;
import com.example.security_log_system.dto.ThreatResponseDto;
import com.example.security_log_system.entity.DetectedThreat;
import com.example.security_log_system.repository.ThreatRepository;
import com.example.security_log_system.service.ThreatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Threat API", description = "위협 데이터 조회 및 관리 API")
@RestController
@RequestMapping("/api/v1/threats")
@RequiredArgsConstructor
public class ThreatController {


    private final ThreatService threatService;

    // 전체 위협 조회
    @GetMapping
    public ResponseEntity<Page<ThreatResponseDto>> getAllThreats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "detectedAt")
        );

        return ResponseEntity.ok(threatService.getAllThreats(pageable));
    }

    // AI 결과를 HTTP로 직접 수신하는 보조/테스트용 API
    // 운영의 기본 AI 결과 처리 흐름은 ai-result-topic을 사용한다.
    @PostMapping("/detect")
    public ResponseEntity<String> receiveDetect(@Valid @RequestBody ThreatDto threatDto){
        threatService.saveDetectedThreat(threatDto);
        return ResponseEntity.ok("Threat recorded successfully.");
    }
}

/*
TODO
    - DetectedThreat Entity를 직접 반환하지 말고 ThreatResponseDto 사용
    - findAll() 대신 최신순/pagination 적용
    - /detect가 실제 운영 흐름인지, 테스트용 API인지 문서에 명확히 표시
    - ThreatDto에 @Valid 검증 추가
    - Kafka 기반 AI 결과 처리와 /detect HTTP 처리 역할을 구분해서 정리
    - Controller에서 Repository 직접 접근 대신 ThreatService로 조회 로직 이동
* */