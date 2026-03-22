package com.example.security_log_system.service;

import com.example.security_log_system.dto.ThreatDto;
import com.example.security_log_system.entity.DetectedThreat;
import com.example.security_log_system.entity.IpBlacklist;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.BlacklistRepository;
import com.example.security_log_system.repository.LogRepository;
import com.example.security_log_system.repository.ThreatRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 받은 메시지를 분석하고 LogRepository를 통해 DB에 저장.
@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final ThreatRepository threatRepository;
    private final BlacklistRepository blacklistRepository;


    // JSON 파싱을 위한 객체 추가
    private final ObjectMapper objectMapper = new ObjectMapper();

    // IP별 403 에러 횟수 저장 (IP, 횟수)
    private final Map<String,Integer> errorCounter = new ConcurrentHashMap<>();
    // 차단 임계치 설정
    private static final int BLOCK_THRESHOLD = 5;

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

            // 💡 줄바꿈 기호 (\r)만 들어오거나 빈 값인 경우 처리 중단
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

                // 2. 블랙리스트 체크
                blacklistRepository.findByIpAddress(ip).ifPresent(blacklist->{
                    System.err.println("[차단] 블랙리스트 IP 접근: "+ip);
                });

                // 3. 위협 탐지 및 저장 (일단 임의로 403 에러가 나면 위협으로 기록)
                if(status ==403){
                    saveThreat(entry,"Forbidden Access","MEDIUM","허가되지 않은 경로 접근 시도");

                    // 해당 IP의 에러 횟수 증가
                    int count = errorCounter.merge(ip,1,Integer::sum);
                    System.out.println("[감시] IP: " + ip + " | 403 에러 누적: " + count + "회");

                    // 임계치 도달 시 블랙리스트 등록
                    if(count >= BLOCK_THRESHOLD) {
                        if (blacklistRepository.findByIpAddress(ip).isEmpty()) {
                            blacklistRepository.save(IpBlacklist.builder()
                                    .ipAddress(ip)
                                    .reason("403 Forbidden 접근 시도")
                                    .dangerLevel(3)
                                    .createdAt(LocalDateTime.now())
                                    .build());
                            System.out.println("[블랙리스트 등록] 403 경로로 5번 접근하였으므로, 블랙리스트에 IP 추가됨: " + ip);
                            errorCounter.remove(ip);  // 차단 후 카운터 초기화
                        }
                    }else if(status == 200){
                        // 정상 접속 시 카운트를 조금 깎아주거나 초기화 하는 로직을 넣으면 더 정교해짐.
                        errorCounter.remove(ip);
                    }
                }

            } else {
                // 파싱에 실패한 경우에도 원본은 저장 (디버깅용)
                logRepository.save(LogEntry.builder().rawLog(message).createdAt(LocalDateTime.now()).build());
                System.out.println("파싱 실패, 원본만 저장: "+message);
            }
        } catch(Exception e) {
            System.err.println("에러 발생: "+e.getMessage());
        }
    }

    // 임의로 만든 보안 위협탐지
    private void saveThreat(LogEntry entry, String type, String severity, String description){
        DetectedThreat threat = DetectedThreat.builder()
                .logEntry(entry)        // log_id를 위해 부모 객체인 entry를 통째로 전달
                .threatType(type)
                .severity(severity)
                .description(description)
                .build();
        threatRepository.save(threat);
    }

    /*
    * AI 서버가 분석해서 보낸 탐지 결과를 DB에 저장합니다.
    * ThreatController -> LogService.saveDetectedThreat 호출
    * */
    public void saveDetectedThreat(ThreatDto threatDto){
        // (선택사항) 해당 IP의 최근 로그를 찾아 연결하는 로직
        // 1. 지금은  간단하게 AI가 준 정보 위주로 저장

        DetectedThreat threat = DetectedThreat.builder()
                .threatType(threatDto.getThreatType())
                .severity(mapRiskLevelToSeverity(threatDto.getDangerLevel()))
                .description(threatDto.getDescription())
                .build();

        threatRepository.save(threat);
        System.out.println("[AI 탐지 기록] 새로운 위협이 등록되었습니다: "+threatDto.getThreatType());

        // 2. 위험도가 높으면 자동으로 블랙리스트 등록
        if(threatDto.getDangerLevel()>=4){
            if(blacklistRepository.findByIpAddress(threatDto.getClientIp()).isEmpty()){
                blacklistRepository.save(IpBlacklist.builder()
                        .ipAddress(threatDto.getClientIp())
                        .reason("AI 탐지 위협: "+threatDto.getThreatType())
                        .dangerLevel(threatDto.getDangerLevel())
                        .createdAt(LocalDateTime.now())
                        .build());


                System.out.println("[자동 차단] 고위험 IP 블랙리스트 등록: "+threatDto.getClientIp());
            }
        }

    }

    // 위험도 숫자를 (1~5) 를 "HIGH", "CRITICAL" 등의 문자열로 바꿔주는 편의 메서드
    private String mapRiskLevelToSeverity(int level){
        if(level>=4) return "CRITICAL";
        if(level>=3) return "HIGH";
        if(level>=2) return "MEDIUM";
        return "LOW";
    }

}
