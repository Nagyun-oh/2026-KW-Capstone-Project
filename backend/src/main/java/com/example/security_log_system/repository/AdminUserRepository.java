package com.example.security_log_system.repository;

import com.example.security_log_system.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository<AdminUser, Long> -> AdminUser 테이블을 다루고, PK타입은 Long
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    // Optional -> 결과가 없을 수도 있으니 null 대신 Optional로 감싸서 반환
    Optional<AdminUser> findByUsername(String username);
}

/*
TODO
    - AuthController에서 직접 Repository를 쓰기보다 AuthService로 이동
    - username 중복 여부는 Entity unique 제약 + Service 검증을 같이 사용
    - 관리자 계정 조회 외에 role 기반 조회가 필요할지 검토
* */