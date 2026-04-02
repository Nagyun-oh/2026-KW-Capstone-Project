package com.example.security_log_system.config;


import com.example.security_log_system.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration  // 이 클래스는 설정 파일
@EnableWebSecurity  // Spring Security 활성화
@RequiredArgsConstructor // final 필드 생성자 자동 생성 (JwtFilter 주입)
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    // SecurityFilterChain
    // -> 어떤 요청을 허용/차단할지 규칙을 정하는 곳
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF 비활성화
            // CSRF = 브라우저 기반 공격 방어기능인데
            // JWT 방식은 세션을 안쓰므로 필요 없음 -> 끄기
                .csrf(csrf -> csrf.disable())
            // 2. 세션 사용 안함
            // JWT는 서버가 로그인 상태를 기억 안해도 됨
            // 요청마다 토큰으로 본인 증명하니까
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 3. 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**", // 로그인/회원가입 -> 누구나 접근 가능
                                "/swagger-ui/**",           // API 문서 -> 누구나 접근 가능
                                "/v3/api-docs/**",          // API 문서 -> 누구나 접근 가능
                                "/health"                   // 서버 상태 확인 -> 누구나 접근 가능
                        ).permitAll()                       // 위의 경로들은 토큰 없어도 허용
                        .anyRequest().authenticated()       // 나머지는 토큰 필수
                )
            // 4. JWT 필터 등록
            // UsernamePasswordAuthenticationFilter 실행 전에 JwtFilter 먼저 실행
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 비밀번호 암호화 도구
    // DB에 비밀번호 저장할때 사용
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    // 로그인 처리할 때 사용하는 매니저
    // AuthService에서 로그인 검증할때 주입받아 씀
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }

}

/*
## 필터 실행 순서
[요청 들어옴]
      ↓
[JwtFilter]     ← 토큰 검사해서 사용자 인증 처리
      ↓
[UsernamePasswordAuthenticationFilter]  ← 기존 아이디/비밀번호 필터 (우리는 패스)
      ↓
[권한 체크] → 인증 안 됐으면 403/401 반환
      ↓
[컨트롤러]
* */

/*
## 전체 요약

| 설정 | 내용 |
| CSRF disable | JWT 사용하므로 불필요 |
| STATELESS | 서버가 세션 안 만듦, 토큰으로만 인증 |
| permitAll 경로 | 로그인, Swagger, 헬스체크는 토큰 없이 접근 가능 |
| authenticated | 그 외 모든 요청은 토큰 필수 |
| JwtFilter 등록 | 모든 요청에서 토큰 먼저 검사 |
| BCrypt | 비밀번호 단방향 암호화 (복호화 불가) |

---

## 흐름 예시
# 로그인 요청 (토큰 없어도 됨)
POST /api/v1/auth/login  →  permitAll → 통과 ✅

# 로그 조회 요청 (토큰 필요)
GET /api/v1/logs
  ├─ 토큰 있음 → JwtFilter 통과 → 조회 ✅
  └─ 토큰 없음 → 401 Unauthorized ❌

* */