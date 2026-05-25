package com.example.security_log_system.service;



import com.example.security_log_system.dto.AiRequestDto;
import com.example.security_log_system.dto.AiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate;
    private static final String AI_API_URL ="http://localhost:8000/predict";

    public AiResponseDto analyze(AiRequestDto request){
        try{
            return restTemplate.postForObject(
                    AI_API_URL, // 어디로 보낼지
                    request,    // 뭘 보낼지 : AiRequestDto 객체를 자동으로 JSON으로 변환해서 보냄
                    AiResponseDto.class // 응답을 어떤 타입으로 받을지 : FastAPI가 JSON으로 응답하면 -> 자동으로 AiResponseDto 객체로 변환해줘
            );
        }catch (Exception e){
            System.err.println("[AI 연동 오류] "+e.getMessage());
            return null;
        }
    }
}

/*
내 Spring 서버  →  restTemplate  →  AI FastAPI 서버
* */