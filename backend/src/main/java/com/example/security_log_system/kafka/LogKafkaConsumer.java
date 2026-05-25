package com.example.security_log_system.kafka;


import com.example.security_log_system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// 실제 로그를 수신해서 LogService에 전달
// log-topic consume
// logConsumer : Kafka에서 메시지만 받아서 logService 호출


@Component
@RequiredArgsConstructor
public class LogKafkaConsumer {
    private final LogService logService;

    @KafkaListener(topics = "log-topic",groupId="log-group",containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message){
        try {
            // LogService에서 트랜잭션이 시작됨.
            logService.processRawLog(message);
        } catch (Exception e) {
            // 여기서 에러를 잡아야 카프카 리스너가 죽지 않고 다음 로그를 계속 처리함.
            System.err.println("[Kafka Consumer Error] 메시지 처리 실패: "+e.getMessage());
            // (심화) 실패한 메시지는 따로 'dead-letter-topic'으로 보낼 수도 있음.
        }
    }

}
