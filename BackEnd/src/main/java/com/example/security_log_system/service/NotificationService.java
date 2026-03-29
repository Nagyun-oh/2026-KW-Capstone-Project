package com.example.security_log_system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;  // 웹소켓 전송 도구

    // 비동기 처리: 슬랙이나 메일 전송이 늦어져도 로그 저장은 바로 완료됨
    @Async
    public void sendUrgentAlert(String ip, String type, int level){
        // 지금은 콘솔 로그로 실시간 알림 시뮬레이션
        System.out.println("🚨 [REAL-TIME ALERT] 고위험군 위협 감지!");
        System.out.println("🚨 대상 IP: "+ip+"유형: "+type+" | 위험도: "+level);

        // 웹소켓 실시간 전송 (리액트 대시보드로 데이터 전송)
        Map<String,Object> payload = new HashMap<>();
        payload.put("ip",ip);
        payload.put("type",type);
        payload.put("level",level);
        payload.put("time", LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/threats",payload);
    }

}
