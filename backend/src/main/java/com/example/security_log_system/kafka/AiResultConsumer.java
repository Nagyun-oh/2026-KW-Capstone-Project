package com.example.security_log_system.kafka;

// ai-result-topic comsume

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
            System.err.println("[AI Result Consumer Error] Invalid message format: "+message);
        }catch (Exception e){
            System.err.println("[AI Result Consumer Error] Failed to save AI result: "+e.getMessage());
        }
    }

}
