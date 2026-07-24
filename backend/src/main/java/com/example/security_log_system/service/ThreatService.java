package com.example.security_log_system.service;

import com.example.security_log_system.dto.AiResponseDto;
import com.example.security_log_system.dto.ThreatDto;
import com.example.security_log_system.dto.ThreatResponseDto;
import com.example.security_log_system.entity.DetectedThreat;
import com.example.security_log_system.entity.IpBlacklist;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.BlacklistRepository;
import com.example.security_log_system.repository.LogRepository;
import com.example.security_log_system.repository.ThreatRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
@Transactional
public class ThreatService {

    private final LogRepository logRepository;
    private final ThreatRepository threatRepository;
    private final BlacklistService blacklistService;
    private final NotificationService notificationService;

    private final Map<String, Integer> errorCounter = new ConcurrentHashMap<>();    // IP별 403 에러 횟수 저장 (IP, 횟수)
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public Page<ThreatResponseDto> getAllThreats(Pageable pageable){
        return threatRepository.findAll(pageable)
                .map(ThreatResponseDto::from);
    }


    /*
     * API 테스트용
     * */
    public void saveDetectedThreat(ThreatDto threatDto) {

        DetectedThreat threat = DetectedThreat.builder()
                .threatType(threatDto.getThreatType())
                .severity(mapRiskLevelToSeverity(threatDto.getDangerLevel()))
                .description(threatDto.getDescription())
                .build();

        threatRepository.save(threat);
        incrementThreatDetectedMetric(threat.getSeverity());
        System.out.println("[API 테스트] 새로운 위협이 등록되었습니다: " + threatDto.getThreatType());

        // 위험도가 높으면 자동으로 블랙리스트 등록
        if (threatDto.getDangerLevel() >= 4) {
            blacklistService.addToBlacklist(threatDto.getClientIp(), " 직접 POST: " + threatDto.getThreatType()
                    , threatDto.getDangerLevel());

            notificationService.sendUrgentAlert(threatDto.getClientIp()," 직접 POST",threatDto.getDangerLevel());
            System.out.println("[API 테스트] 고위험 IP 블랙리스트 등록: " + threatDto.getClientIp());
        }

    }

    /*
    * Kafka 테스트용
    * */
    public void saveAiDetectedThreat(AiResponseDto aiResponse) {
        if (aiResponse.getLogId() ==null){
            throw new IllegalArgumentException("AI response logId is null");
        }

        LogEntry entry = logRepository.findById(aiResponse.getLogId())
                .orElseThrow(() -> new IllegalArgumentException(
            "LogEntry not found"
        ));

        if("0.0.0.0".equals(aiResponse.getIpAddress())){
            return;
        }
        DetectedThreat threat = DetectedThreat.builder()
                .logEntry(entry)
                .threatType("AI Detection")
                .severity(aiResponse.getThreatScore() >= 0.8 ? "CRITICAL" : "HIGH")
                .description(aiResponse.getReason())
                .detectedAt(LocalDateTime.now())
                .build();
        threatRepository.save(threat);
        incrementThreatDetectedMetric(threat.getSeverity());

        if(aiResponse.getThreatScore() >= 0.8){
            blacklistService.addToBlacklist(aiResponse.getIpAddress(), aiResponse.getReason(), 4);
            notificationService.sendUrgentAlert(aiResponse.getIpAddress(), "AI Detection",4);
        }
    }

    private void incrementThreatDetectedMetric(String severity){
        Counter.builder("security.threats.detected")
                .description("Number of detected security threats")
                .tag("severity",severity)
                .register(meterRegistry)
                .increment();
    }

    // 위험도 숫자를 (1~5) 를 "HIGH", "CRITICAL" 등의 문자열로 바꿔주는 편의 메서드
    private String mapRiskLevelToSeverity(int level) {
        if (level >= 4) return "CRITICAL";
        if (level >= 3) return "HIGH";
        return "MEDIUM";
    }

}

/*
TODO
    - blacklistRepository, IpBlacklist, Not import는 현재 사용되지 않으므로 제거
    - 규칙 기반 탐지 analyzeLogEntry를 사용할지 제거할지 결정
    - threatScore 값을 DetectedThreat에 저장할지 검토
    - severity 문자열을 Enum으로 변경 검토
    - saveDetectedThreat와 saveAiDetectedThreat 역할 구분 문서화
    - System.out 대신 Logger 사용
    - AI 응답 검증 추가: score 범위, reason null 처리
*/

