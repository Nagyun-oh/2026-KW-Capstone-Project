package com.example.security_log_system.service;


import com.example.security_log_system.dto.AiRequestDto;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.kafka.AiRequestProducer;
import com.example.security_log_system.repository.LogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
    Kafka 메시지 수신
    -> JSON의 "log" 필드 추출
    -> Nginx 로그 파싱
    -> LogEntry 생성
    -> DB 저장 요청
    -> AiRequestDto 생성
    -> AI 분석 요청
    -> 블랙리스트 확인
    까지 확인

* */

@ExtendWith(MockitoExtension.class)
public class LogServiceTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private ThreatService threatService;

    @Mock
    private BlacklistService blacklistService;

    @Mock
    private AiRequestProducer aiRequestProducer;

    @InjectMocks
    private LogService logService;

    @Test
    @DisplayName("유효한 Nginx 로그 수신 시 로그를 파싱하여 저장하고 AI 분석 요청을 전송한다")
    void validNginxLogMessage_thenParseAndSaveLogEntry(){
        // given
        String message = "{\"log\":\"127.0.0.1 - - [07/May/2026:16:00:00 +0900] \\\"GET /admin HTTP/1.1\\\" 403\"}";

        // NullPointerException 에러 방지용 코드
        when(logRepository.save(any(LogEntry.class)))
                .thenAnswer(invocation -> {
                    LogEntry logEntry = invocation.getArgument(0);
                    ReflectionTestUtils.setField(logEntry,"id",1L);
                    return logEntry;
                });


        ArgumentCaptor<LogEntry> logCaptor = ArgumentCaptor.forClass(LogEntry.class);
        ArgumentCaptor<AiRequestDto> aiCaptor = ArgumentCaptor.forClass(AiRequestDto.class);

        // when
        logService.processRawLog(message);

        // then
        verify(logRepository).save(logCaptor.capture());

        LogEntry savedLog = logCaptor.getValue();
        assertThat(savedLog.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(savedLog.getRequestMethod()).isEqualTo("GET");
        assertThat(savedLog.getRequestUrl()).isEqualTo("/admin");
        assertThat(savedLog.getStatusCode()).isEqualTo(403);

        verify(aiRequestProducer).sendAnalysisRequest(aiCaptor.capture());

        AiRequestDto aiRequestDto = aiCaptor.getValue();
        assertThat(aiRequestDto.getLogId()).isEqualTo(1L);
        assertThat(aiRequestDto.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(aiRequestDto.getMethod()).isEqualTo("GET");
        assertThat(aiRequestDto.getUrlPath()).isEqualTo("/admin");

    }

    @Test
    @DisplayName("빈 로그 메시지 수신 시 로그를 저장하거나 AI 분석을 요청하지 않는다")
    void emptyLogMessage_thenDoNothing(){
        // given
        String message = "{\"log\":\"\"}";

        // when
        logService.processRawLog(message);

        // then
        verify(logRepository,never()).save(any());
        verify(aiRequestProducer,never()).sendAnalysisRequest(any());

    }


}
