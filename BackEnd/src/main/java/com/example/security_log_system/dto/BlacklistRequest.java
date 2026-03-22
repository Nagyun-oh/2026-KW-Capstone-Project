package com.example.security_log_system.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BlacklistRequest {
    private String ipAddress;  //차단할 IP
    private String reason; // 차단 사유 (ex: 반복적인 어드민 페이지 접근)
    private int dangerlevel;
}
