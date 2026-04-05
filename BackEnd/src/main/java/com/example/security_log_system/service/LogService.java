package com.example.security_log_system.service;

import com.example.security_log_system.dto.AiRequestDto;
import com.example.security_log_system.dto.AiResponseDto;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.LogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 받은 메시지를 분석하고 LogRepository를 통해 DB에 저장.
@Transactional  // 데이터 무결성을 위해 스프링 AOP기반 트랜잭션 관리 적용
@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final ThreatService threatService;
    private final BlacklistService blacklistService;
    private final AiService aiService;


    // JSON 파싱을 위한 객체 추가
    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
     *  topics = "log-topic"  : log-topic라는 이름의 카프카 토픽에 쌓이는 메시지를 실시간으로 감시.
     *  groupId = "log-group" : consumer 그룹 ID, 여러 대의 서버가 같은 그룹 ID로 동작하면 메시지를 분산해서 처리
     *  processRawLog(String message) : 카프카 로그가 들어오면 이 메서드가 실행
     * */
    public void processRawLog(String kafkaMessage) {

        // try-catch : 파싱 중 에러나 DB 저장 중 에러가 발생해도 프로그램이 죽지 않고 에러 메시지만 출력하도록 방어적으로 설계
        try{
            // 1. Fluent bit가 보낸 JSON에서 "log"필드만 추출
            JsonNode jsonNode = objectMapper.readTree(kafkaMessage);
            String message = jsonNode.get("log").asText();

            // 2. 줄바꿈 기호 (\r)만 들어오거나 빈 값인 경우 처리 중단
            if(message ==null || message.trim().isEmpty() || message.equals("\r")){
                return;
            }

            // 정규표현식: Nginx 기본 로그 형식을 분석합니다.
            // 예시 로그 : 127.0.0.1 - - [14/Mar/2026] "GET /admin HTTP/1.1" 403
            String regex = "^(\\S+) - - \\[(.*?)\\] \"(\\S+) (\\S+) .*?\" (\\d+)";
            Pattern pattern = Pattern.compile(regex);   // 미리 정의한 regex(정규표현식 문자열)을 컴퓨터가 해석하기 쉬운 형태인 Pattern 객체로 컴파일 (성능개선 부분)
            Matcher matcher = pattern.matcher(message); // 위에서 만든 pattern과 message(로그)를 비교하여 Matcher 객체 생성 (대조할 수 있는 상태 완성)

            // 정규 표현식 패턴이 맞는지 확인
            if (matcher.find()) {
                // 각 그룹별로 데이터를 추출합니다.
                String ip  = matcher.group(1);      // 127.0.0.1
                String method = matcher.group(3);   // GET
                String url = matcher.group(4);      // admin
                int status = Integer.parseInt(matcher.group(5));    //403

                // 1. 로그 저장
                LogEntry entry = logRepository.save(LogEntry.builder()
                        .ipAddress(ip)
                        .requestMethod(method)
                        .requestUrl(url)
                        .statusCode(status)
                        .createdAt(LocalDateTime.now())
                        .build());
                System.out.println("[DB] network_logs에서 파싱 및 저장 완료: "+url +" ("+status+")");

                //  AI 서버로 분석 요청
                AiRequestDto aiRequest = AiRequestDto.builder()
                        .method(method)
                        .urlPath(url)
                        .queryParams("")        // Nginx 로그엔 없으면 빈 값
                        .bodyContent("")
                        .userAgent("")
                        .ipAddress(ip)
                        .timestamp(LocalDateTime.now().toString())
                        .build();

                AiResponseDto aiResponse = aiService.analyze(aiRequest);

                //  AI가 위협으로 판단하면 (threatScore >= 0.5 → ipAddress가 실제 IP)
                if (aiResponse != null && !"0.0.0.0".equals(aiResponse.getIpAddress())) {
                    threatService.saveAiDetectedThreat(aiResponse, entry);
                }

                /*// 위협 탐지 및 저장 (일단 임의로 403 에러가 나면 위협으로 기록)
                if(entry.getStatusCode() ==403){
                    threatService.analyzeLogEntry(entry);
                }*/

                // 블랙리스트 체크
                blacklistService.isBlocked(entry.getIpAddress());
            }
        } catch(JsonProcessingException e) {
            // 로그  파싱 실패 시 런타임 예외로 던져서 전체 트랙잭션 롤백 유도
            throw new RuntimeException("Kafka 메시지 파싱 에러: ",e);
        }
    }

}
