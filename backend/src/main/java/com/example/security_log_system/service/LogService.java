package com.example.security_log_system.service;

import com.example.security_log_system.config.StaticResourceFilterProperties;
import com.example.security_log_system.dto.AiRequestDto;
import com.example.security_log_system.dto.LogDetailResponseDto;
import com.example.security_log_system.dto.LogResponseDto;
import com.example.security_log_system.dto.LogSearchCondition;
import com.example.security_log_system.entity.LogEntry;
import com.example.security_log_system.exception.LogNotFoundException;
import com.example.security_log_system.kafka.AiRequestProducer;
import com.example.security_log_system.repository.LogRepository;
import com.example.security_log_system.repository.LogSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class LogService {

    private static final String MASKED_VALUE = "***";
    private static final Pattern SENSITIVE_FORM_FIELD = Pattern.compile(
            "(?i)(^|&)((?:password|passwd|token|access_token|refresh_token|api_key|secret|session)[^=]*)=([^&]*)"
    );

    private final LogRepository logRepository;
    private final BlacklistService blacklistService;
    private final AiRequestProducer aiRequestProducer;
    private final StaticResourceFilterProperties staticResourceFilterProperties;

    // 목록에는 요약 정보만 반환해 대용량 audit JSON의 반복 전송을 피한다.
    @Transactional(readOnly = true)
    public Page<LogResponseDto> getLogs(LogSearchCondition condition, Pageable pageable){
        return logRepository
                .findAll(LogSpecification.search(condition),pageable)
                .map(LogResponseDto::from);
    }

    @Transactional(readOnly = true)
    public LogDetailResponseDto getLog(Long logId) {
        return logRepository.findById(logId)
                .map(LogDetailResponseDto::from)
                .orElseThrow(() -> new LogNotFoundException(logId));
    }


    // JSON 파싱을 위한 객체 추가
    private final ObjectMapper objectMapper;

    public void processRawLog(String kafkaMessage) {
        try{
            JsonNode jsonNode = objectMapper.readTree(kafkaMessage);

            // ModSecurity JSON audit log
            if(jsonNode.has("transaction")){
                processModSecurityAuditJson(jsonNode);
                return;
            }

            // {"log" : "..."} 테스트
            if(jsonNode.has("log")){
                processNginxLog(jsonNode.get("log").asText());
                return;
            }

            // 실제 WAF Fluent Bit JSON 테스트
            if(jsonNode.has("remote_addr") && jsonNode.has("method") && jsonNode.has("path")){
                processWafAccessJson(jsonNode);
                return ;
            }

            // dev_tools 테스트
            if(jsonNode.has("method") && jsonNode.has("url_path")) {
                processAiInputJson(jsonNode);
                return;
            }
            log.warn("Unsupported Kafka message format. length={}",kafkaMessage.length());
        } catch(JsonProcessingException exception) {
            // 로그  파싱 실패 시 런타임 예외로 던져서 전체 트랙잭션 롤백 유도
            throw new RuntimeException("Kafka message parsing error: ",exception);
        }
    }

    private void processModSecurityAuditJson(JsonNode auditNode) {
        JsonNode transaction = auditNode.path("transaction");
        JsonNode request = transaction.path("request");
        JsonNode response = transaction.path("response");

        String ipAddress = transaction.path("client_ip").asText("0.0.0.0");
        String method = request.path("method").asText("");
        String fullPath = request.path("uri").asText("/");
        String bodyContent = sanitizeBody(request.path("body").asText(""));
        String userAgent = findHeaderIgnoreCase(request.path("headers"), "User-Agent");
        int statusCode = response.path("http_code").asInt(0);

        String urlPath = fullPath;
        String queryParams = "";
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex >= 0) {
            urlPath = fullPath.substring(0, queryIndex);
            queryParams = fullPath.substring(queryIndex + 1);
        }

        // 경로만 신뢰하지 않고 서버가 반환한 Content-Type까지 함께 확인한다.
        String responseContentType = findHeaderIgnoreCase(response.path("headers"), "Content-Type");
        if (shouldSkipStaticResource(
                method,
                urlPath,
                queryParams,
                bodyContent,
                statusCode,
                responseContentType,
                transaction.path("messages")
        )) {
            log.debug("Safe static resource skipped. method={}, path={}, contentType={}",
                    method, urlPath, responseContentType);
            return;
        }

        JsonNode sanitizedAuditNode = sanitizeAuditLog(auditNode);
        LogEntry entry = logRepository.save(LogEntry.builder()
                .ipAddress(ipAddress)
                .requestMethod(method)
                .requestUrl(fullPath)
                .statusCode(statusCode)
                .rawLog(sanitizedAuditNode.toString())
                .createdAt(LocalDateTime.now())
                .build());

        AiRequestDto aiRequest = AiRequestDto.builder()
                .logId(entry.getId())
                .method(method)
                .urlPath(urlPath)
                .queryParams(queryParams)
                .bodyContent(bodyContent)
                .userAgent(userAgent)
                .urlLen(fullPath.length())
                .specialCharCount(countSpecialChars(fullPath + " " + bodyContent))
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now().toString())
                .build();

        // 보수적인 정적 리소스 필터를 통과하지 않은 요청은 AI 분석 대상으로 전달한다.
        aiRequestProducer.sendAnalysisRequest(aiRequest);
    }

    private boolean shouldSkipStaticResource(
            String method,
            String urlPath,
            String queryParams,
            String bodyContent,
            int statusCode,
            String responseContentType,
            JsonNode wafMessages
    ) {
        if (!staticResourceFilterProperties.isEnabled()) {
            return false;
        }

        // 서비스 설정(메서드·경로·응답 형식)을 모두 만족해야 정적 요청 후보가 된다.
        boolean allowedMethod = staticResourceFilterProperties.getMethods().stream()
                .anyMatch(configuredMethod -> configuredMethod.equalsIgnoreCase(method));
        boolean knownStaticPath = staticResourceFilterProperties.getPathPrefixes().stream()
                .anyMatch(urlPath::startsWith);
        boolean staticContentType = staticResourceFilterProperties.getContentTypes().stream()
                .anyMatch(configuredType -> responseContentType
                        .toLowerCase()
                        .startsWith(configuredType.toLowerCase()));
        boolean knownStaticExtension = staticResourceFilterProperties.getExtensions().stream()
                .anyMatch(extension -> urlPath.toLowerCase().endsWith(extension.toLowerCase()));
        boolean staticRepresentation = staticContentType
                || (statusCode == 304 && knownStaticExtension);
        // WAF 탐지나 우회 가능성이 조금이라도 있으면 로그와 AI 분석을 유지한다.
        boolean safeStatus = statusCode == 200 || statusCode == 304;
        boolean noWafDetection = wafMessages.isArray() && wafMessages.isEmpty();
        boolean canonicalPath = !urlPath.contains("..")
                && !urlPath.contains("%")
                && !urlPath.contains(";")
                && !urlPath.contains("\\")
                && urlPath.indexOf('\0') < 0;

        return allowedMethod
                && knownStaticPath
                && staticRepresentation
                && safeStatus
                && noWafDetection
                && canonicalPath
                && queryParams.isBlank()
                && bodyContent.isBlank();
    }

    private String findHeaderIgnoreCase(JsonNode headers, String headerName) {
        if (!headers.isObject()) {
            return "";
        }
        Iterator<Map.Entry<String, JsonNode>> fields = headers.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getKey().equalsIgnoreCase(headerName)) {
                return field.getValue().asText("");
            }
        }
        return "";
    }

    private JsonNode sanitizeAuditLog(JsonNode auditNode) {
        JsonNode copy = auditNode.deepCopy();
        JsonNode request = copy.path("transaction").path("request");
        sanitizeHeaders(request.path("headers"));
        sanitizeHeaders(copy.path("transaction").path("response").path("headers"));
        if (request instanceof ObjectNode requestObject && request.has("body")) {
            requestObject.put("body", sanitizeBody(request.path("body").asText("")));
        }
        return copy;
    }

    private void sanitizeHeaders(JsonNode headers) {
        if (headers instanceof ObjectNode headerObject) {
            headerObject.fields().forEachRemaining(field -> {
                if (isSensitiveField(field.getKey())) {
                    headerObject.put(field.getKey(), MASKED_VALUE);
                }
            });
        }
    }

    private String sanitizeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode bodyJson = objectMapper.readTree(body);
            redactSensitiveFields(bodyJson);
            return objectMapper.writeValueAsString(bodyJson);
        } catch (JsonProcessingException ignored) {
            return SENSITIVE_FORM_FIELD.matcher(body).replaceAll("$1$2=" + MASKED_VALUE);
        }
    }

    private void redactSensitiveFields(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fields().forEachRemaining(field -> {
                if (isSensitiveField(field.getKey())) {
                    objectNode.put(field.getKey(), MASKED_VALUE);
                } else {
                    redactSensitiveFields(field.getValue());
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::redactSensitiveFields);
        }
    }

    private boolean isSensitiveField(String fieldName) {
        String normalized = fieldName.toLowerCase().replace("-", "_");
        return normalized.equals("authorization")
                || normalized.equals("proxy_authorization")
                || normalized.equals("cookie")
                || normalized.equals("set_cookie")
                || normalized.contains("password")
                || normalized.equals("passwd")
                || normalized.contains("token")
                || normalized.contains("api_key")
                || normalized.equals("apikey")
                || normalized.contains("secret")
                || normalized.contains("session");
    }

    private void processNginxLog(String message) {

        // 2. 줄바꿈 기호 (\r)만 들어오거나 빈 값인 경우 처리 중단
        if (message == null ||message.isBlank()) {
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
            String ip = matcher.group(1);      // 127.0.0.1
            String method = matcher.group(3);   // GET
            String url = matcher.group(4);      // admin
            int status = Integer.parseInt(matcher.group(5));    //403

            if (blacklistService.isBlocked(ip)) {
                return;
            }

            // 1. 로그 저장
            LogEntry entry = logRepository.save(LogEntry.builder()
                    .ipAddress(ip)
                    .requestMethod(method)
                    .requestUrl(url)
                    .statusCode(status)
                    .rawLog(message)
                    .createdAt(LocalDateTime.now())
                    .build());

            //  AI 서버로 분석 요청
            AiRequestDto aiRequest = AiRequestDto.builder()
                    .logId(entry.getId())
                    .method(method)
                    .urlPath(url)
                    .queryParams("")        // Nginx 로그엔 없으면 빈 값
                    .bodyContent("")
                    .userAgent("")
                    .ipAddress(ip)
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            aiRequestProducer.sendAnalysisRequest(aiRequest);
        }
    }

    private int countSpecialChars(String text){
        if(text== null || text.isBlank()){
            return 0;
        }

        String[] specialChars = {"'","\"","<",">","--",";","%"};
        int count = 0;

        for(String specialChar : specialChars){
            int index = 0;
            while((index = text.indexOf(specialChar,index)) >=0 ){
                count++;
                index +=specialChar.length();
            }
        }

        return count;
    }

    private void processWafAccessJson(JsonNode jsonNode){
        String ipAddress = jsonNode.path("remote_addr").asText("0.0.0.0");
        String method = jsonNode.path("method").asText("");
        String fullPath = jsonNode.path("path").asText("/");
        int statusCode = jsonNode.path("status").asInt(0);
        String userAgent = jsonNode.path("http_user_agent").asText("");

        String urlPath = fullPath;
        String queryParams = "";

        int queryIndex = fullPath.indexOf("?");
        if(queryIndex >= 0){
            urlPath = fullPath.substring(0,queryIndex);
            queryParams = fullPath.substring(queryIndex+1);
        }

        // 입력된 로그 DB에 저장
        LogEntry entry = logRepository.save(LogEntry.builder()
                .ipAddress(ipAddress)
                .requestMethod(method)
                .requestUrl(fullPath)
                .statusCode(statusCode)
                .rawLog(jsonNode.toString())
                .createdAt(LocalDateTime.now())
                .build());

        // AI 요청 DTO 생성
        AiRequestDto aiRequest = AiRequestDto.builder()
                .logId(entry.getId())
                .method(method)
                .urlPath(urlPath)
                .queryParams(queryParams)
                .bodyContent("")
                .userAgent(userAgent)
                .urlLen(fullPath.length())
                .specialCharCount(countSpecialChars(fullPath))
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now().toString())
                .build();

        aiRequestProducer.sendAnalysisRequest(aiRequest);
    }

    private void processAiInputJson(JsonNode jsonNode) {
        String method = jsonNode.path("method").asText();
        String urlPath = jsonNode.path("url_path").asText();
        String queryParams = jsonNode.path("query_params").asText("");
        String bodyContent = jsonNode.path("body_content").asText("");
        String userAgent = jsonNode.path("user_agent").asText("");
        String ipAddress = jsonNode.path("ip_address").asText("127.0.0.1");
        String timestamp = jsonNode.path("timestamp").asText(LocalDateTime.now().toString());

        String requestUrl = queryParams.isBlank()
                ? urlPath
                : urlPath + "?" + queryParams;

        LogEntry entry = logRepository.save(LogEntry.builder()
                .ipAddress(ipAddress)
                .requestMethod(method)
                .requestUrl(requestUrl)
                .statusCode(200)
                .rawLog(jsonNode.toString())
                .createdAt(LocalDateTime.now())
                .build());

        AiRequestDto aiRequest = AiRequestDto.builder()
                .logId(entry.getId())
                .method(method)
                .urlPath(urlPath)
                .queryParams(queryParams)
                .bodyContent(bodyContent)
                .userAgent(userAgent)
                .urlLen(jsonNode.path("url_len").asInt(0))
                .specialCharCount(jsonNode.path("special_char_count").asInt(0))
                .ipAddress(ipAddress)
                .timestamp(timestamp)
                .build();

        aiRequestProducer.sendAnalysisRequest(aiRequest);
    }
}
