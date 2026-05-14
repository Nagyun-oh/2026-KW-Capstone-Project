package com.example.security_log_system.service;


import com.example.security_log_system.dto.AiRequestDto;
import com.example.security_log_system.dto.AiResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/* Backend- AI 연동 테스트
* 
    1. AiService가 FastAPI /predict를 호출하는지
    2. 정상 응답을 AiResponseDto로 반환하는지
    3. FastAPI 연결 실패 시 null로 안전하게 처리하는지
*  */

@ExtendWith(MockitoExtension.class)
public class AiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiService aiService;

    /*
    *  analyze() 를 호출하면 FastAPI /predict 엔드포인트롤 호출하고,
    *  응답값을 그대로 반환하는지 확인한다.
    * */
    @Test
    void analyze_thenCallFastApiPredictEndPoint(){
        // given
        
        // 요청 DTO 만들기
        AiRequestDto request= AiRequestDto.builder()
                .method("GET")
                .urlPath("/rest/products/search")
                .queryParams("q=%27%20OR%201%3D1--")
                .bodyContent("")
                .userAgent("Mozilla/5.0")
                .ipAddress("127.0.0.1")
                .timestamp("2026-05-07T16:00:00")
                .build();

        // 가짜 AI 응답 만들기
        /*
        ReflectionTestUtils.setField()를 쓰는 이유는
        AiResponseDto에 setter가 없고 private 필드 + getter만 있어서,
        테스트에서 값을 강제로 넣기 위함.
        * */
        AiResponseDto response = new AiResponseDto();
        ReflectionTestUtils.setField(response,"threatScore",0.91f);
        ReflectionTestUtils.setField(response,"ipAddress","127.0.0.1");
        ReflectionTestUtils.setField(response,"reason","URL 공격 키워드");

        // RestTemplate 동작 지정
        when(restTemplate.postForObject(
                eq("http://localhost:8000/predict"),
                eq(request),
                eq(AiResponseDto.class)
        )).thenReturn(response);

        // when : aiService.analyze()가 내부적으로 RestTemplate를 잘 사용하는지 확인
        AiResponseDto result = aiService.analyze(request);

        // then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getThreatScore()).isEqualTo(0.91f);
        assertThat(result.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(result.getReason()).isEqualTo("URL 공격 키워드");
        
        /*
        * AiService가 /predict 주소로
        * request를 보내고
        * AiResponseDto.class로 응답을 받으려 했는지 검증
        * */
        verify(restTemplate).postForObject(
                eq("http://localhost:8000/predict"),
                eq(request),
                eq(AiResponseDto.class)
        );

    }

    /*
    * FastAPI 서버 호출 중 예외가 발생하면,
    * AiService.analyze()가 null을 반환하는지 확인한다.
    * 이 테스트는 AI 서버가 죽어도 Spring 전체가 죽지 않게 방어 처리가 되어 있는지 확인
    * */
    @Test
    void analyze_whenFastApiError_thenReturnNull(){
        // given : 간단한 AI 요청 데이터 생성
        AiRequestDto request = AiRequestDto.builder()
                .method("GET")
                .urlPath("/admin")
                .ipAddress("127.0.0.1")
                .timestamp("2026-05-07T16:00:00")
                .build();

        // FastAPI 에러 상황 가정
        /*
        * restTemplate.postForObject(아무 URL, 이 request, AiResponseDto.class)가 호출되면
        * 실제 HTTP 요청을 보내지 말고
        * RuntimeException을 던져라.
        * */
        when(restTemplate.postForObject(
                anyString(),    // 어떤 문자열 URL이 들어오든 상관없다.
                eq(request)
                ,eq(AiResponseDto.class)
        )).thenThrow(new RuntimeException("FastAPI connection failed"));

        // when
        AiResponseDto result = aiService.analyze(request);

        // then
        assertThat(result).isNull();


    }


}
