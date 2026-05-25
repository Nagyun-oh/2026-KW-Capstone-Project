package com.example.security_log_system.service;


import com.example.security_log_system.dto.AiRequestDto;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.LogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private AiService aiService;

    @InjectMocks
    private LogService logService;

    // 유효한 Nginx 로그 메시지가 들어오면 파싱해서 LogEntry로 저장
    @Test
    void validNginxLogMessage_thenParseAndSaveLogEntry(){
        // given
        String message = "{\"log\":\"127.0.0.1 - - [07/May/2026:16:00:00 +0900] \\\"GET /admin HTTP/1.1\\\" 403\"}";

        // NullPointerException 에러 방지용 코드
        when(logRepository.save(any(LogEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


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

        verify(aiService).analyze(aiCaptor.capture());

        AiRequestDto aiRequestDto = aiCaptor.getValue();
        assertThat(aiRequestDto.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(aiRequestDto.getMethod()).isEqualTo("GET");
        assertThat(aiRequestDto.getUrlPath()).isEqualTo("/admin");

      verify(blacklistService).isBlocked("127.0.0.1");

    }

    @Test
    void emptyLogMessage_thenDoNothing(){
        // given
        String message = "{\"log\":\"\"}";

        // when
        logService.processRawLog(message);

        // then
        verify(logRepository,never()).save(any());
        verify(aiService,never()).analyze(any());

    }


}
