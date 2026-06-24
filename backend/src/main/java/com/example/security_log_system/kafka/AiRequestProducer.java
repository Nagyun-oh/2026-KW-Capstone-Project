package com.example.security_log_system.kafka;

// ai-request-topic produce

import com.example.security_log_system.dto.AiRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiRequestProducer {

    private static final String AI_REQUEST_TOPIC = "ai-request-topic";

    private final KafkaTemplate<String,String> kafkaTemplate;   // Kafka 전송 담당
    private final ObjectMapper objectMapper;                    // AiRequestDto를 JSON 문자열로 변환하는 담당

    public void sendAnalysisRequest(AiRequestDto request){
        try {
            String message = objectMapper.writeValueAsString(request);
            kafkaTemplate.send(AI_REQUEST_TOPIC, String.valueOf(request.getLogId()),message);
            System.out.println("[Kafka Producer] AI analysis request sent. logId= "+request.getLogId());
        } catch (JsonProcessingException e){
            throw new RuntimeException("AI analysis request serialization failed.",e);
        }

    }
}

/*
TODO
    - topic 이름을 application.yml로 이동
    - Kafka 전송 실패 콜백 처리 추가
    - System.out 대신 Logger 사용
    - 직렬화 실패 시 RuntimeException만 던지지 말고 상위 처리 정책 정리
    - AI 요청 메시지 스키마를 docs에 문서화
*/