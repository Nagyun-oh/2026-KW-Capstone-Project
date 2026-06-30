package com.example.security_log_system.controller;

import com.example.security_log_system.dto.LoginRequest;
import com.example.security_log_system.entity.AdminUser;
import com.example.security_log_system.repository.AdminUserRepository;
import com.example.security_log_system.security.JwtUtil;
import com.example.security_log_system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController                 // REST API Controller
@RequestMapping("/api/v1/auth") // Controller의 기본 URL
@RequiredArgsConstructor        // final 필드에 대한 생성자를 Lombok이 자동 생성
public class AuthController {

    private final AuthService authService;
    
    /*
    POST /api/v1/auth/login
        Controller
        → authService.login(request)
        → 성공하면 token 응답
        → 실패하면 401 응답
    * */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request){
       return authService.login(request)
               .<ResponseEntity<?>>map(token -> ResponseEntity.ok(Map.of("token",token)))
               .orElseGet(()-> ResponseEntity.status(401).body("Invalid username or password."));
    }

    // 회원가입 API (테스트용)
    /* POST api/v1/auth/register */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody LoginRequest request){
        if(!authService.registerAdmin(request)){
            return ResponseEntity.badRequest().body("Username already exists.");
        }
        return ResponseEntity.ok("Admin account created successfully.");
    }
}


