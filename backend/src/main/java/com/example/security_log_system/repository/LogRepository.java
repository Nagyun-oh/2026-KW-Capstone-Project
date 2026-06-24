package com.example.security_log_system.repository;

import com.example.security_log_system.entity.LogEntry;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;


/*  전체 방문 기록(로그) 관리*/
public interface LogRepository  extends JpaRepository<LogEntry,Long> {
    Page<LogEntry> findByIpAddress(String ipAddress,Pageable pageable);
}

/*
TODO
    - 깨진 주석 수정
    - findAll() 대신 최신순 조회 메서드 추가
      예: findTop100ByOrderByCreatedAtDesc()
    - IP 검색도 최신순 정렬 적용
      예: findByIpAddressOrderByCreatedAtDesc(String ipAddress)
    - 대시보드용 pagination 조회 추가
    - 로그 조회 로직을 Controller에서 Service로 이동
* */