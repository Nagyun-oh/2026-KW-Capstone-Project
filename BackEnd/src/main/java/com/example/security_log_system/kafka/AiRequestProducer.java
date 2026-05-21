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

    private final KafkaTemplate<String,String> kafkaTemplate;
    private final ObjectMapper objectMapper;

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
