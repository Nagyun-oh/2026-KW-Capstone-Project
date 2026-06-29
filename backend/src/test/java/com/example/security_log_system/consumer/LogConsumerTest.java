package com.example.security_log_system.consumer;


import com.example.security_log_system.kafka.LogKafkaConsumer;
import com.example.security_log_system.service.LogService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/* Kafka 메시지가 들어왔을 때 LogConsumer가 LogService로 넘기는지 확인*/
/* 즉, Consumer 계층이 Service 계층으로 메시지를 잘 전달하는지 확인하는 테스트 */

@ExtendWith(MockitoExtension.class)
public class LogConsumerTest {

    // 가짜 객체 생성
    @Mock
    private LogService logService;

    // 의존성 자동 주입
    @InjectMocks
    private LogKafkaConsumer logConsumer;

    @Test
    @DisplayName("1. Kafka 로그 메시지를 LogService에 전달한다")
    void kafkaMessageReceived_thenCallLogService(){
        // given
        String message = """
                {"log":127.0.0.1 GET /admin 403}""
                """;

        // when
        logConsumer.consume(message);

        // then
        verify(logService).processRawLog(message);
    }

    @Test
    @DisplayName("2. 로그 처리에 실패해도 Consumer 예외를 외부로 던지지 않는다")
    void consume_whenLogServiceFails_thenDoNotPropagateException(){

        // given
        String message = "invalid-log-message";

        doThrow(new RuntimeException("log processing failed"))
                .when(logService)
                .processRawLog(message);

        // when & then
        assertThatCode( () -> logConsumer.consume(message))
                .doesNotThrowAnyException();

        verify(logService).processRawLog(message);
    }
}
/*
    Kafka 메시지 수신
    → LogService.processRawLog() 호출
    → RuntimeException 발생
    → LogKafkaConsumer의 catch에서 처리
    → Consumer 외부에는 예외가 전달되지 않음
* */