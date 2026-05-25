package com.example.security_log_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

// Spring -> FastAPI 요청용
@Getter
@Builder
public class AiRequestDto {
    private String method;

    @JsonProperty("url_path")
    private String urlPath;

    @JsonProperty("query_params")
    private String queryParams;

    @JsonProperty("body_content")
    private String bodyContent;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("url_len")
    private int urlLen;

    @JsonProperty("special_char_count")
    private int specialCharCount;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("timestamp")
    private String timestamp;
}

/*

        method:             str         # "GET" | "POST" | "PUT" | "DELETE"
        url_path:           str         # "/tienda1/publico/login.jsp"
        query_params:       str = ""    # URL 파라미터 (?뒤)
        body_content:       str = ""    # POST body 내용
        user_agent:         str = ""    # User-Agent 헤더값
        url_len:            int = 0     # URL 전체 길이 (0이면 자동 계산)
        special_char_count: int = 0     # 특수문자 개수  (0이면 자동 계산)
    # ── 추가 컬럼 ──────────────────────────────────────
        ip_address:         str = ""    # 요청 IP  (ex. "192.168.0.10")
        timestamp:          str = ""    # 요청 시각 (ex. "2026-03-31T14:00:00")*/
