package com.example.security_log_system.repository;


import com.example.security_log_system.entity.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/* 차단된 IP들을 관리하는 용도입니다. */
@Repository
public interface BlacklistRepository
        extends JpaRepository<IpBlacklist,Long>,
        JpaSpecificationExecutor<IpBlacklist> {
    Optional<IpBlacklist> findByIpAddress(String ipAddress);
}

/*
TODO
    - ipAddress 중복 방지를 위해 Entity에 unique 제약 추가 검토
    - 만료되지 않은 IP만 조회하는 메서드 추가 검토
      예: findByIpAddressAndExpiredAtAfter(...)
    - 전체 블랙리스트 조회 시 최신순 정렬 메서드 추가
*/