package com.example.security_log_system.consumer;


import com.example.security_log_system.kafka.LogKafkaConsumer;
import com.example.security_log_system.service.LogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/* Kafka 메시지가 들어왔을 때 LogConsumer가 LogService로 넘기는지 확인*/
/* 즉, Consumer 계층이 Service 계층으로 메시지를 잘 전달하는지 확인하는 테스트 */

@ExtendWith(MockitoExtension.class)
public class LogKafkaConsumerTest {

    // 가짜 객체 생성
    @Mock
    private LogService logService;

    // 의존성 자동 주입
    @InjectMocks
    private LogKafkaConsumer logKafkaConsumer;

    @Test
    void kafkaMessageReceived_thenCallLogService(){
        // given
        String message = "{\"log\":\"127.0.0.1 - - [07/May/2026:16:00:00 +0900] \\\"GET /admin HTTP/1.1\\\" 403\"}";

        // when
        logKafkaConsumer.consume(message);

        // then
        verify(logService).processRawLog(message);

    }
}
