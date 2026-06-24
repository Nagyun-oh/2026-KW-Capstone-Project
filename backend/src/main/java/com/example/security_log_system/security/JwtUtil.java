package com.example.security_log_system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

// @Component가 붙어 있어서 Spring Bean으로 등록됨 -> AuthController, JwtFilter에서 주입받아 사용할 수 있게 됨.
@Component
public class JwtUtil {

    // JWT 서명에 사용하는 비밀키
    private final SecretKey key = Keys.hmacShaKeyFor(
            "security-log-system-secret-key-256bit!!".getBytes()
    );

    // 토큰 만료시간
    private final long EXPIRATION_MS = 1000 * 60 * 60*24;

    // 토큰 생성
    // 결과: "eyJhbGciOiJIUzI1..." 같은 긴 문자열
    public String generateToken(String username){
        return Jwts.builder()
                .subject(username)      // 토큰 안에 username 저장
                .issuedAt(new Date())   // 발급 시간
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS)) // 만료 시간 : 지금 + 24시간
                .signWith(key)          // 비밀키로 서명 (위조 방지)
                .compact();             // 문자열로 변환해서 반환
    }

    // 토큰에서 username 꺼내기
    public String extractUsername(String token){
        return Jwts.parser()
                .verifyWith(key)            // 비밀키로 서명 검증
                .build()
                .parseSignedClaims(token)   // 토큰 파싱
                .getPayload()
                .getSubject();              // subject(username) 꺼내기
    }

    // 토큰 검증
    // - 토큰이 정상이고, 서명이 맞고, 만료되지 않았으면 true
    // - 변조되었거나 만료되었거나 형식이 이상하면 false
    public boolean validateToken(String token){
        try{
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        }catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }
}

/*
TODO
    - JWT secret key를 코드에서 제거하고 환경변수/application.yml로 이동
    - 만료 시간도 설정값으로 분리
    - username뿐 아니라 role도 token claim에 포함할지 검토
    - validateToken에서 만료/변조/형식 오류 로그 구분
    - 테스트 코드 추가: 생성, 검증, 만료, 잘못된 토큰
*/