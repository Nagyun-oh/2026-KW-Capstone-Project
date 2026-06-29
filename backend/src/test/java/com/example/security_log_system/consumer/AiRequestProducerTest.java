package com.example.security_log_system.consumer;

import com.example.security_log_system.dto.AiRequestDto;
import com.example.security_log_system.kafka.AiRequestProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiRequestProducerTest {

    @Mock
    private KafkaTemplate<String,String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiRequestProducer Producer;

    @Test
    @DisplayName("1.AI 요청 DTO를 JSON으로 변환하여 Kafka로 전송한다")
    void sendAnalysisRequest_thenSendJsonMessage() throws Exception {

        // given
        AiRequestDto request = AiRequestDto.builder()
                .logId(1L)
                .method("GET")
                .urlPath("/admin")
                .ipAddress("192.168.0.10")
                .build();

        String message = """
                {"log_id":1,"method":"GET","url_path":"/admin"}
                """;

        when(objectMapper.writeValueAsString(request)).thenReturn(message);

        // when
        Producer.sendAnalysisRequest(request);

        // then
        verify(objectMapper).writeValueAsString(request);

        verify(kafkaTemplate).send(
                "ai-request-topic",
                "1",
                message
        );
    }

    @Test
    @DisplayName("2.JSON 직렬화에 실패하면 RuntimeException을 발생시킨다")
    void sendAnalysisRequest_whenSerializationFails_thenThrowsException() throws  Exception{

        // given
        AiRequestDto request = AiRequestDto.builder()
                .logId(1L)
                .method("GET")
                .build();
        // when
        when(objectMapper.writeValueAsString(request))
                .thenThrow(new JsonProcessingException(
                        "Serialization failed") {});

        // when & then
        Assertions.assertThatThrownBy(
                () -> Producer.sendAnalysisRequest(request)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AI analysis request serialization failed.")
                .hasCauseInstanceOf(JsonProcessingException.class);

        verifyNoInteractions(kafkaTemplate);
    }
}

/*
    AiRequestDto
    → ObjectMapper.writeValueAsString()
    → JSON 문자열
    → KafkaTemplate.send(topic, logId, message)
* */
