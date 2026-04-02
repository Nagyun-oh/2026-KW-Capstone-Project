package com.example.security_log_system.repository;

import com.example.security_log_system.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/* DB에 쿼리 날리는 도구 */

// JpaRepository<AdminUser, Long>
//  -> AdminUser 테이블을 다루고, PK타입은 Long
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    // "SELECT * FROM admin_user WHERE username = ? " 쿼리를 자동 생성해줌
    // Optional -> 결과가 없을 수도 있으니 null 대신 Optional로 감싸서 반환
    Optional<AdminUser> findByUsername(String username);
}
