package com.example.security_log_system.controller;

import com.example.security_log_system.exception.GlobalExceptionHandler;
import com.example.security_log_system.service.BlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 이 테스트는 실제 서버를 띄우지 않고 BlacklistController 의 HTTP 요청과 검증 동작을 확인함.

@ExtendWith(MockitoExtension.class)
public class BlacklistControllerTest {

    // 실제 Service 대신 mock 객체를 사용함. 따라서 DB에는 접근하지 않는다.
    @Mock
    private BlacklistService blacklistService;

    // 가상의 HTTP 요청을 Controller에 보내는 Spring 테스트 도구
    private MockMvc mockMvc;

    // 각 테스트 메서드가 실행되기 전에 매번 호출됨.
    @BeforeEach
    void setup() {

        // @Valid, @NotBlank, @ValidIpAddress 등을 실행할 Validator를 생성하고 초기화한다.
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // mock Service를 주입해서 테스트 대상 Controller를 직접 생성한다.
        BlacklistController controller = new BlacklistController(blacklistService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)       // 해당 Controller만 테스트 환경에 등록
                .setControllerAdvice(new GlobalExceptionHandler())  // Validation 예외를 GlobalExceptionHandler가 처리하도록 등록
                .setValidator(validator)                            // DTO 검증에 사용할 Validator 등록
                .build();                                           // MockMvc 생성
    }


    // 1. 유효한 IP 테스트
    // 하나의 테스트를 여러 입력값으로 반복 실행한다.
    @ParameterizedTest
    @ValueSource(strings = {
            "192.168.0.10",
            "2001:db8::1"
    })
    @DisplayName("유효한 IPv4 또는 IPv6 등록 시 200 OK를 반환하고 블랙리스트 서비스가 호출된다")
    void addBlacklist_whenIpAddressIsValid_thenReturnOk(String ipAddress) throws Exception {

        // Mock Service 동작 설정
        when(blacklistService.addToBlacklist(
                ipAddress,
                "Repeated attack",
                4
        )).thenReturn(true);

        // HTTP 요청 생성 (Controller 응답이 200 OK 인지 검사)
        mockMvc.perform(post("/api/v1/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ipAddress": "%s",
                              "reason": "Repeated attack",
                              "dangerLevel": 4
                            }
                            """.formatted(ipAddress)))
                .andExpect(status().isOk());

        // Controller가 Service에 정확한 값을 전달했는지 검증
        verify(blacklistService).addToBlacklist(
                ipAddress,
                "Repeated attack",
                4
        );
    }

    // 2. 잘못된 IP 테스트
    @Test
    @DisplayName("유효하지 않은 IP 등록 시 검증 오류와 400 Bad Request를 반환하고 서비스는 호출되지 않는다")
    void addBlacklist_whenIpAddressIsInvalid_thenReturnBadRequest() throws Exception {

        // HTTP 요청 생성
        mockMvc.perform(post("/api/v1/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ipAddress": "999.999.999.999",
                                  "reason": "Repeated attack",
                                  "dangerLevel": 4
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ipAddress")
                        .value("Invalid IPv4 or IPv6 address."));

        // Validation이 Controller 실행 전에 실패했으므로 Service가 한번도 호출되지 않았는지 확인.
        verifyNoInteractions(blacklistService);
    }
}

/*

유효한 IP:
    MockMvc 요청
        → JSON을 BlacklistRequest로 변환
        → IP Validation 통과
        → Controller 실행
        → mock Service 호출
        → 200 OK

 잘못된 IP:
     MockMvc 요청
        → JSON을 BlacklistRequest로 변환
        → IP Validation 실패
        → GlobalExceptionHandler 실행
        → 400 Bad Request
        → Service 호출 안 됨

* */