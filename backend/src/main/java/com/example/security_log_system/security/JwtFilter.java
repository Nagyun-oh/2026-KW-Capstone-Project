package com.example.security_log_system.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

// @Component: Spring Bean 등록.
// @RequiredArgsConstructor: JwtUtil 생성자 주입.
// OncePerRequestFilter: 요청 한 번당 필터가 한 번만 실행되게 해주는 Spring 필터 클래스.
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    // JWT 검증을 위해 JwtUtil 사용
    private final JwtUtil jwtUtil;

    // HTTP 요청이 Controller에 도착하기 전에 실행되는 메서드
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        // 요청 헤더에서 "Authorizaion" 값 꺼냄
        // ex) "Bearer eyJhbGcoi...."
        String header = request.getHeader("Authorization");

        if(header !=null && header.startsWith("Bearer ")){

            // "Bearer " 7글자 제거하여 , 순수 토큰만 추출
            String token = header.substring(7);

            // 토큰 검증
            if(jwtUtil.validateToken(token)){

                /* 현재 요청을 보낸 사용자는 인증된 사용자이고,
                username은 토큰에서 꺼낸 값이며,
                권한은 ROLE_ADMIN이다. */

                String username = jwtUtil.extractUsername(token);   // 인증된 사용자 이름

                UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        username,null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

                // 현재 요청의 인증 정보를 Spring Security Context에 등록
                // 이후 Controller나 Security 권한 체크에서 “이 사용자는 인증됨”으로 판단할 수 있음.
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request,response); // 다음 필터로 요청 전달
    }

}

/*
TODO
    - Authorization 오타 주석 수정
    - 깨진 한글 주석 수정
    - ROLE_ADMIN을 하드코딩하지 말고 토큰 claim 또는 DB에서 가져오도록 개선
    - SecurityContext에 WebAuthenticationDetailsSource로 request detail 추가 검토
    - 인증 실패 원인을 로그로 남길지 검토
    - permitAll 경로에서는 필터 처리 최소화할지 검토
* */