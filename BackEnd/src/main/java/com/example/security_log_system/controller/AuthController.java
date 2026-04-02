package com.example.security_log_system.controller;

import com.example.security_log_system.dto.LoginRequest;
import com.example.security_log_system.entity.AdminUser;
import com.example.security_log_system.repository.AdminUserRepository;
import com.example.security_log_system.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController     // JSON 응답을 반환하는 컨트롤러
@RequestMapping("/api/v1/auth") // 이 컨트롤러의 기본 URL
@RequiredArgsConstructor
public class AuthController {
    private final AdminUserRepository adminUserRepository; // DB 접근
    private final PasswordEncoder passwordEncoder;         // 비밀번호 암호화
    private final JwtUtil jwtUtil;                         // 토큰 생성

    @PostMapping("/login")  //  POST /api/v1/auth/login
    // <?> : 어떤 타입이든 반환 가능 , 요청 Body를 객체로 변환
    public ResponseEntity<?> login(@RequestBody LoginRequest request){

        // 1. DB에서 username으로 유저 찾기
        AdminUser user = adminUserRepository.findByUsername(request.getUsername())
                .orElse(null); // 없으면 null 반환

        // 2. 유저 없거나 비밀번호 털리면 401
        // passwordEncoder.matches("입력한 비번", "DB에 저장된 암호화된 비번")
        if(user ==null || !passwordEncoder.matches(request.getPassword(),user.getPassword())){
            return ResponseEntity.status(401).body("아이디 또는 비밀번호가 틀렸습니다.");
        }

        // 3. 토큰 생성해서 반환
        // 응답: { "token": "eyJhbGci..." }
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(Map.of("token",token));
    }

    // 테스트용 관리자 계정 생성 (개발 중에만 사용, 나중에 삭제)
    // 회원가입 API (테스트용)
    @PostMapping("/register")   // POST api/v1/auth/register
    public ResponseEntity<?> register(@RequestBody LoginRequest request){

        // 1. 이미 존재하는 username인지 확인
        if(adminUserRepository.findByUsername(request.getUsername()).isPresent()){
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
        }

        // 2. 새 관리자 계정 생성
        AdminUser user = AdminUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_ADMIN")
                .build();

        adminUserRepository.save(user);
        return ResponseEntity.ok("관리자 계정 생성 완료");
    }
}

/*
## 전체 로그인 흐름

# 로그인 성공 흐름
클라이언트                         서버
    │                               │
    │  POST /api/v1/auth/login      │
    │  { username, password }  ───► │
    │                               ├─ DB에서 유저 찾기
    │                               ├─ 비밀번호 검증
    │                               ├─ JWT 토큰 생성
    │  { "token": "eyJ..." }   ◄─── │
    │                               │
    │  이후 모든 요청 헤더에 토큰 첨부   │
    │  Authorization: Bearer eyJ... │
    │                          ───► │ (JwtFilter가 검증)

---

## 응답 코드 정리

| 상황 | 응답 코드 | 응답 내용 |
| 로그인 성공 | 200 OK | `{ "token": "eyJ..." }` |
| 아이디/비번 틀림 | 401 Unauthorized | `아이디 또는 비밀번호가 틀렸습니다.` |
| 중복 아이디 | 400 Bad Request | `이미 존재하는 아이디입니다.` |
| 회원가입 성공 | 200 OK | `관리자 계정 생성 완료` |


## 비밀번호 암호화 포인트

저장할 때  "1234"  →  encode()  →  "$2a$10$xyz..."  DB 저장
검증할 때  "1234"  →  matches() →  "$2a$10$xyz..."  비교 → true/false

⚠️ BCrypt는 단방향 암호화 → 복호화 불가능
   그래서 매번 다시 암호화해서 비교하는 방식 사용

* */