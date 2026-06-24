package com.example.security_log_system.service;

import com.example.security_log_system.dto.LoginRequest;
import com.example.security_log_system.entity.AdminUser;
import com.example.security_log_system.repository.AdminUserRepository;
import com.example.security_log_system.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AdminUserRepository adminUserRepository; // 관리자 계정 DB 조회/저장
    private final PasswordEncoder passwordEncoder;         // 비밀번호 암호화 및 검증
    private final JwtUtil jwtUtil;                         // 로그인 성공 시 JWT 토큰 생성

    /*
     로그인 성공하면 JWT token을 담은 Optional 반환.
     실패하면 Optional.empty() 반환.
    * */
    @Transactional(readOnly = true)
    public Optional<String> login(LoginRequest request){
        return adminUserRepository.findByUsername(request.getUsername())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> jwtUtil.generateToken(user.getUsername()));
    }

    /*
      회원가입 성공하면 true, 중복이면 false
    * */
    @Transactional
    public boolean registerAdmin(LoginRequest request){
        if(adminUserRepository.findByUsername(request.getUsername()).isPresent()){
            return false;
        }

        AdminUser user = AdminUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_ADMIN")
                .build();

        adminUserRepository.save(user);
        return true;
    }

}
