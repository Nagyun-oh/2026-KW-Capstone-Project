package com.example.security_log_system.repository;

import com.example.security_log_system.entity.DetectedThreat;
import com.example.security_log_system.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/* 적발된 위협 내용 관리 */
@Repository
public interface ThreatRepository extends JpaRepository<DetectedThreat,Long> {
    // 나중에 특정 로그 ID로 탐지된 위협들을 찾을 때 사용.
    List<DetectedThreat> findByLogEntryId(Long logId);
}
