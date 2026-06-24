package com.example.security_log_system.controller;


import com.example.security_log_system.dto.LogResponseDto;
import com.example.security_log_system.exception.GlobalExceptionHandler;
import com.example.security_log_system.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class LogControllerTest {

    @Mock
    private LogService logService;

    private MockMvc mockMvc;

    // 각 테스트 메서드가 실행되기 전에 매번 호출됨.
    @BeforeEach
    void setup() {

        // @Valid, @NotBlank, @ValidIpAddress 등을 실행할 Validator를 생성하고 초기화한다.
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // mock Service를 주입해서 테스트 대상 Controller를 직접 생성한다.
        LogController controller = new LogController(logService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)       // 해당 Controller만 테스트 환경에 등록
                .setControllerAdvice(new GlobalExceptionHandler())  // Validation 예외를 GlobalExceptionHandler가 처리하도록 등록
                .setValidator(validator)                            // DTO 검증에 사용할 Validator 등록
                .build();                                           // MockMvc 생성
    }
    
    // 전체 로그 조회 테스트:
    //          전체 로그 조회가 Page 형태로 정상 반환되는지 확인한다.
    @Test
    @DisplayName("전체 로그 조회 시 페이지네이션된 로그와 200 OK를 반환한다")
    void getAllLogs_thenReturnPagedLogs() throws Exception {

        // Pageable 생성
        Pageable pageable  = PageRequest.of(
                0,                                      // 첫 번째 페이지
                20,                                                 // 페이지당 20개
                Sort.by(Sort.Direction.DESC,"createdAt") // 생성 시각 최신순
        );

        // 테스트 응답 데이터 생성
        LogResponseDto log = createLogResponse("192.168.0.10");

        // Page 객체 생성
        //  List.of(log) → 현재 페이지에 들어갈 데이터
        //  pageable     → 페이지 번호, 크기, 정렬 정보
        //  1            → 전체 데이터 개수
        Page<LogResponseDto> result = new PageImpl<>(List.of(log),pageable,1);

        // Mock 동작 설정
        when(logService.getAllLogs(eq(pageable)))
                .thenReturn(result);

        // GET 요청 ( GET /api/v1/logs?page=0&size=20 )
        mockMvc.perform(get("/api/v1/logs")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ipAddress")
                        .value("192.168.0.10"))
                .andExpect(jsonPath("$.content[0].requestMethod")
                        .value("GET"))
                .andExpect(jsonPath("$.content[0].statusCode")
                        .value(200))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
        
        // Service 호출 검증
        verify(logService).getAllLogs(pageable);
    }

    // IP 검색 테스트
    // 동일한 테스트를 IPv4와 IPv6값으로 각각 실행
    @ParameterizedTest
    @ValueSource(strings = {
            "192.168.0.10",
            "2001:db8::1"
    })
    @DisplayName("유효한 Ipv4 또는 Ipv6으로 검색 시 페이지네이션된 로그를 반환한다")
    void getLogsByIp_whenIpIsValid_thenReturnPagedLogs(String ipAddress) throws Exception {

        // Pageable 생성
        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 테스트 응답 데이터 생성
        LogResponseDto log = createLogResponse(ipAddress);
        
        // Page 객체 생성
        Page<LogResponseDto> result =
                new PageImpl<>(List.of(log), pageable, 1);
        
        // Mock 동작 설정
        when(logService.getLogByIp(ipAddress, pageable))
                .thenReturn(result);

        // 검색 요청 ( GET /api/v1/logs/search?ip=192.168.0.10&page=0&size=20 )
        mockMvc.perform(get("/api/v1/logs/search")
                        .param("ip", ipAddress)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ipAddress")
                        .value(ipAddress))
                .andExpect(jsonPath("$.totalElements").value(1));

        // Service 호출 검증
        verify(logService).getLogByIp(ipAddress, pageable);
    }

    private LogResponseDto createLogResponse(String ipAddress){
        return LogResponseDto.builder()
                .id(1L)
                .ipAddress(ipAddress)
                .requestMethod("GET")
                .requestUrl("/admin")
                .statusCode(200)
                .rawLog("sample log")
                .createdAt(LocalDateTime.now())
                .build();
    }
}

/*
    1. 전체 로그 조회 성공
    2. 유효한 IPv4/IPv6 검색 성공
    3. Service에 pagination 정보가 정확히 전달되는지

    MockMvc GET 요청
    → LogController
    → mock LogService
    → 테스트용 Page 반환
    → JSON 변환
    → 상태 코드와 응답 필드 검증
    → Service 호출 인자 검증
* */