package com.example.security_log_system.consumer;

import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.LogRepository;
import com.example.security_log_system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// logConsumer : Kafka에서 메시지만 받아서 logService 호출
@Component
@RequiredArgsConstructor
public class LogConsumer {
    private final LogService logService;

    @KafkaListener(topics = "log-topic",groupId="log-group")
    public void consume(String message){
        logService.processRawLog(message);
    }

}
