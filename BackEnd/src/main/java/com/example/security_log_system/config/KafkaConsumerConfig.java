package com.example.security_log_system.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;    //  카프카용 Deserializer로 바꿔주어야 에러가 안 납니다!
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka // 스프링에게 "나 카프카 리스너 쓸 거야!"라고 선언
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 1. 카프카 서버 주소 (우리 본진 위치)
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // 2. 소비자 그룹 (같은 그룹끼리 로그를 나눠서 처리함)
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "log-group");

        // 3. 역직렬화(Deserializer): 카프카에 저장된 '0101' 바이트 데이터를 '자바 문자열'로 변환
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 실제 카프카 메시지를 낚아채는 '낚싯대(ContainerFactory)' 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // (중요) 병렬 처리 스레드 수 설정 가능 (로그가 폭주할 때 유용!)
        // factory.setConcurrency(3);
        return factory;
    }
}