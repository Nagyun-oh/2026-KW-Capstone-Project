package com.example.security_log_system.kafka;


import com.example.security_log_system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogKafkaConsumer {

    private final LogService logService;

    @KafkaListener(topics = "log-topic",groupId="log-group",containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message){
        try {
            logService.processRawLog(message);
        } catch (Exception e) {
            // 여기서 에러를 잡아야 카프카 리스너가 죽지 않고 다음 로그를 계속 처리함.
            System.err.println("[Kafka Consumer Error] 메시지 처리 실패: "+e.getMessage());
        }
    }

}

/*
TODO
    - System.err 대신 Logger 사용
    - 실패 메시지를 dead-letter-topic으로 보내는 구조 검토
    - topic/groupId를 코드에 하드코딩하지 말고 application.yml로 이동
    - Consumer 예외 처리 정책 문서화
*/