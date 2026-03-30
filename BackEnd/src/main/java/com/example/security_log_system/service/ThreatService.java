package com.example.security_log_system.service;

import com.example.security_log_system.dto.ThreatDto;
import com.example.security_log_system.entity.DetectedThreat;
import com.example.security_log_system.entity.IpBlacklist;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.BlacklistRepository;
import com.example.security_log_system.repository.ThreatRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
@Transactional
public class ThreatService {

    private final ThreatRepository threatRepository;
    private final BlacklistRepository blacklistRepository;
    private final BlacklistService blacklistService;
    private final NotificationService notificationService;

    private final Map<String, Integer> errorCounter = new ConcurrentHashMap<>();    // IP별 403 에러 횟수 저장 (IP, 횟수)
    private static final int BLOCK_THRESHOLD = 5;    // 차단 임계치 설정

    public void analyzeLogEntry(LogEntry entry) {
        // 1. 단순 규칙 기반 탐지 (403 에러)
        if (entry.getStatusCode() == 403) {
            saveThreat(threatRepository, entry, "Forbidden Access", "MEDIUM", "허가되지 않은 경로 접근 시도");

            // 해당 IP의 에러 횟수 증가
            int count = errorCounter.merge(entry.getIpAddress(), 1, Integer::sum);
            System.out.println("[감시] IP: " + entry.getIpAddress() + " | 403 에러 누적: " + count + "회");
            if (count >= BLOCK_THRESHOLD) {
                blacklistService.addToBlacklist(entry.getIpAddress(), "403 반복 접근", 3);
                errorCounter.remove(entry.getIpAddress());  // 차단 후 카운터 초기화
            }
        } else if (entry.getStatusCode() == 200) {
            // 정상 접속 시 카운트를 조금 깎아주거나 초기화 하는 로직을 넣으면 더 정교해짐. (추후 추가 예정)
            errorCounter.remove(entry.getIpAddress());
        }
    }


    // DB 위협탐지 테이블에 저장
    private void saveThreat(ThreatRepository threatRepository, LogEntry entry, String type, String severity, String description) {
        DetectedThreat threat = DetectedThreat.builder()
                .logEntry(entry)        // log_id를 위해 부모 객체인 entry를 통째로 전달
                .threatType(type)
                .severity(severity)
                .description(description)
                .build();
        threatRepository.save(threat);
    }

    /*
     * AI 서버가 분석해서 보낸 탐지 결과를 DB에 저장합니다.
     * ThreatController -> LogService.saveDetectedThreat 호출
     * */
    public void saveDetectedThreat(ThreatDto threatDto) {
        // (선택사항) 해당 IP의 최근 로그를 찾아 연결하는 로직
        // 1. 지금은  간단하게 AI가 준 정보 위주로 저장

        DetectedThreat threat = DetectedThreat.builder()
                .threatType(threatDto.getThreatType())
                .severity(mapRiskLevelToSeverity(threatDto.getDangerLevel()))
                .description(threatDto.getDescription())
                .build();

        threatRepository.save(threat);
        System.out.println("[AI 탐지 기록] 새로운 위협이 등록되었습니다: " + threatDto.getThreatType());

        // 2. 위험도가 높으면 자동으로 블랙리스트 등록
        if (threatDto.getDangerLevel() >= 4) {
            blacklistService.addToBlacklist(threatDto.getClientIp(), "AI 탐지 위협/ 지금은 직접 POST: " + threatDto.getThreatType()
                    , threatDto.getDangerLevel());

            notificationService.sendUrgentAlert(threatDto.getClientIp(),"AI 탐지 위협/ 지금은 직접 POST",threatDto.getDangerLevel());
            System.out.println("[자동 차단] 고위험 IP 블랙리스트 등록: " + threatDto.getClientIp());
        }

    }

    // 위험도 숫자를 (1~5) 를 "HIGH", "CRITICAL" 등의 문자열로 바꿔주는 편의 메서드
    private String mapRiskLevelToSeverity(int level) {
        if (level >= 4) return "CRITICAL";
        if (level >= 3) return "HIGH";
        if (level >= 2) return "MEDIUM";
        return "LOW";
    }

}


