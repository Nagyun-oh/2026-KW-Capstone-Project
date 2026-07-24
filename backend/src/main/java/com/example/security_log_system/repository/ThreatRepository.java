package com.example.security_log_system.repository;

import com.example.security_log_system.entity.DetectedThreat;
import com.example.security_log_system.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/* 탐지된 위협 데이터를 저장하고 조회하는 용도입니다. */
@Repository
public interface ThreatRepository
        extends JpaRepository<DetectedThreat,Long>,
        JpaSpecificationExecutor<DetectedThreat> {
    List<DetectedThreat> findByLogEntryId(Long logId);
}

/*
TODO
    - 최신 위협순 조회 메서드 추가
      예: findTop100ByOrderByDetectedAtDesc()
    - severity별 조회 메서드 추가 검토
      예: findBySeverity(String severity)
    - 확인하지 않은 위협 조회 메서드 추가 검토
      예: findByIsCheckedFalse()
    - findByLogEntryId를 사용하는 상세 API 추가 여부 검토
*/