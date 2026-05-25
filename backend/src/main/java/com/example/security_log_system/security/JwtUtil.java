package com.example.security_log_system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

/*
* JWT 토큰 생성/ 검증 도구
* */


@Component // 스프링이 이 클래스를 자동으로 관리해줌 (Bean 등록)
public class JwtUtil {

    // 토큰 서명에 쓸 비밀키 생성
    // "이 토큰은 우리 서버가 만든거야" 를 증명하는 도장 같은 것
    private final SecretKey key = Keys.hmacShaKeyFor(
            "security-log-system-secret-key-256bit!!".getBytes()
    );
    private final long EXPIRATION_MS = 1000 * 60 * 60*24; // 24시간

    // 토큰 생성
    public String generateToken(String username){
        return Jwts.builder()
                .subject(username)      // 토큰 안에 username 저장
                .issuedAt(new Date())   // 발급 시간
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS)) // 만료 시간 : 지금 + 24시간
                .signWith(key)          // 비밀키로 서명 (위조 방지)
                .compact();             // 문자열로 변환해서 반환
        // 결과: "eyJhbGciOiJIUzI1..." 같은 긴 문자열
    }

    // 토큰에서 username 꺼내기
    public String extractUsername(String token){
        return Jwts.parser()
                .verifyWith(key)    // 비밀키로 서명 검증
                .build()
                .parseSignedClaims(token) // 토큰 파싱
                .getPayload()
                .getSubject();            // subject(username) 꺼내기
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token){
        try{
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;    // 파싱 성공 = 유효한 토큰
        }catch (JwtException | IllegalArgumentException e){
            return false;   // 만료되거나 위조된 토큰 = false
        }
    }



}
