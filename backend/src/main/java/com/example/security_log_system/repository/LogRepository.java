package com.example.security_log_system.repository;

import com.example.security_log_system.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*  전체 방문 기록(로그) 관리*/
public interface LogRepository  extends JpaRepository<LogEntry,Long> {
    List<LogEntry> findByIpAddress(String ip);
}

