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

/* 요청마다 토큰 검사하는 필터
*  모든 HTTP 요청이 컨트롤러에 도달하기 전에 이 필터를 거침
*  [클라이언트 요청] -> JwtFilter -> [컨트롤러]
* */


@Component
@RequiredArgsConstructor
// OncePerRequestFilter = 요청 1번당 딱 1번만 실행되는 필터
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        // 요청 헤더에서 "Authorizaion" 값 꺼냄
        // ex) "Bearer eyJhbGcoi...."
        String header = request.getHeader("Authorization");

        if(header !=null && header.startsWith("Bearer ")){

            // "Bearer " 7글자 제거 -> 순수 토큰만 추출
            String token = header.substring(7);

            // 토큰 유효한지 검사
            if(jwtUtil.validateToken(token)){
                String username = jwtUtil.extractUsername(token);

                // 스프링 시큐리티에 "이 사람은 인증된 사용자야" 라고 등록
                UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        username,null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
                // 스프링 시큐리티의 현재 로그인 사용자 보관함에 저장
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request,response); // 다음 단계로 요청 넘기기

    }

}

/*
## 전체 흐름
1. 클라이언트가 요청 보냄
   헤더: Authorization: Bearer eyJhbG...

2. JwtFilter 실행
   ├─ 헤더 없음? → 그냥 통과 (비로그인 요청)
   ├─ 토큰 유효하지 않음? → 그냥 통과 (인증 없이 넘김)
   └─ 토큰 유효함? → SecurityContext에 사용자 등록 후 통과

3. 컨트롤러 도달
   └─ @PreAuthorize 같은 권한 체크에서 SecurityContext 확인
* */
