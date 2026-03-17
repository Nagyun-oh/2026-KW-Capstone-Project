package com.example.security_log_system.repository;


import com.example.security_log_system.entity.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/* 불량 IP 명단관리 */
@Repository
public interface BlacklistRepository extends JpaRepository<IpBlacklist,Long> {
    Optional<IpBlacklist> findByIpAddress(String ipAddress);
}
