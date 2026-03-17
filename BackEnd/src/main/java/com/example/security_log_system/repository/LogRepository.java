package com.example.security_log_system.repository;

import com.example.security_log_system.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/*  전체 방문 기록(로그) 관리*/
public interface LogRepository  extends JpaRepository<LogEntry,Long> {
}

