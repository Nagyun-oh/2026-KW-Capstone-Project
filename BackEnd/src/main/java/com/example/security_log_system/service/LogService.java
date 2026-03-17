package com.example.security_log_system.service;

import com.example.security_log_system.entity.DetectedThreat;
import com.example.security_log_system.entity.IpBlacklist;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.repository.BlacklistRepository;
import com.example.security_log_system.repository.LogRepository;
import com.example.security_log_system.repository.ThreatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 받은 메시지를 분석하고 LogRepository를 통해 DB에 저장.
@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final ThreatRepository threatRepository;
    private final BlacklistRepository blacklistRepository;

    /*
    *  topics = "log-topic"  : log-topic라는 이름의 카프카 토픽에 쌓이는 메시지를 실시간으로 감시.
    *  groupId = "log-group" : consumer 그룹 ID, 여러 대의 서버가 같은 그룹 ID로 동작하면 메시지를 분산해서 처리
    *  processRawLog(String message) : 로그가 들어오면 이 메서드가 실행
    * */
    @KafkaListener(topics = "log-topic", groupId = "log-group")
    public void processRawLog(String message) {

        // try-catch : 파싱 중 에러나 DB 저장 중 에러가 발생해도 프로그램이 죽지 않고 에러 메시지만 출력하도록 방어적으로 설계
        try{
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
                    saveThreat(entry,"Forbidden Access","HIGH","허가되지 않은 경로 접근 시도");

                    // 403 에러를 낸 놈을 블랙리스트에 추가 (테스트용)
                    if(blacklistRepository.findByIpAddress(ip).isEmpty()){
                        blacklistRepository.save(IpBlacklist.builder()
                                .ipAddress(ip)
                                .reason("403 Forbidden 접근 시도")
                                .dangerLevel(3)
                                .createdAt(LocalDateTime.now())
                                .build());
                        System.out.println("[DB 신규 등록] 블랙리스트에 IP 추가됨: "+ip);
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

}
