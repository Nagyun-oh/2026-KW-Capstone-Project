package com.example.security_log_system.kafka;

import com.example.security_log_system.dto.AiResponseDto;
import com.example.security_log_system.service.ThreatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiResultConsumer {

    private final ObjectMapper objectMapper;
    private final ThreatService threatService;

    @KafkaListener(
            topics = "ai-result-topic",
            groupId = "ai-result-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message){
        try{
            AiResponseDto aiResponse = objectMapper.readValue(message, AiResponseDto.class);
            threatService.saveAiDetectedThreat(aiResponse);
            System.out.println("[Kafka Consumer] AI analysis result received. logId= "+aiResponse.getLogId());
        }catch (JsonProcessingException e){
            // JSON 형식 오류
            System.err.println("[AI Result Consumer Error] Invalid message format: "+message);
        }catch (Exception e){
            // 비즈니스 처리/DB 저장 오류
            System.err.println("[AI Result Consumer Error] Failed to save AI result: "+e.getMessage());
        }
    }

}

/*
TODO
    - System.err 대신 Logger 사용
    - 잘못된 AI 결과 메시지를 dead-letter-topic으로 이동 검토
    - topic/groupId를 application.yml로 이동
    - AI 응답 검증 추가: logId, threatScore, ipAddress, reason
    - threat_score 범위 검증 추가
*/