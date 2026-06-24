package com.example.security_log_system.service;



import com.example.security_log_system.dto.AiRequestDto;
import com.example.security_log_system.dto.AiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


/*@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate;
    private static final String AI_API_URL ="http://localhost:8000/predict";

    public AiResponseDto analyze(AiRequestDto request){
        try{
            return restTemplate.postForObject(
                    AI_API_URL, // 어디로 보낼지
                    request,    // 뭘 보낼지 : AiRequestDto 객체를 자동으로 JSON으로 변환해서 보냄
                    AiResponseDto.class // 응답을 어떤 타입으로 받을지 : FastAPI가 JSON으로 응답하면 -> 자동으로 AiResponseDto 객체로 변환
            );
        }catch (Exception e){
            System.err.println("[AI 연동 오류] "+e.getMessage());
            return null;
        }
    }
}*/

/*
TODO
    - 현재 Kafka 기반 AI 연동과 HTTP 직접 호출 방식 중 하나로 정리
    - AI_API_URL을 application.yml로 이동
    - RestTemplate 대신 WebClient 사용 검토
    - AI 서버 장애 시 재시도/timeout 설정 추가
    - 현재 사용되지 않는다면 제거하거나 테스트용으로 명시
* */