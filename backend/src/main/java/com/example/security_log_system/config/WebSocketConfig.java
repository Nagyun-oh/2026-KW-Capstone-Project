package com.example.security_log_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// 실시간 알림용 WebSocket/STOMP 설정 클래스

@Configuration
@EnableWebSocketMessageBroker       // STOMP 기반 WebSocket 메시징을 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 프론트엔드가 WebSocket 연결을 시작할 주소를 등록하는 메서드
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/ws-security")             // 리액트가 접속할 주소
                .setAllowedOrigins("http://localhost:3000")    // CORS 허용
                .withSockJS();
    }

    // 메시지 발행/구독 주소 규칙을 설정하는 메서드
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry){
        registry.enableSimpleBroker("/topic");  // React가 서버 알림을 구독할 때
        registry.setApplicationDestinationPrefixes("/app");    // React가 서버로 보낼 때
    }
}
