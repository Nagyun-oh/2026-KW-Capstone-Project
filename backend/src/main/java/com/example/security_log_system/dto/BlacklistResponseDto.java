package com.example.security_log_system.dto;

import com.example.security_log_system.entity.IpBlacklist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@Builder
public class BlacklistResponseDto {

    private Long id;
    private String ipAddress;
    private String reason;
    private int dangerLevel;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;

    /*
     IpBlacklist Entity
       → BlacklistResponseDto.from(entity)
       → API 응답
    * */
    public static BlacklistResponseDto from(IpBlacklist blacklist){
        return BlacklistResponseDto.builder()
                .id(blacklist.getId())
                .ipAddress(blacklist.getIpAddress())
                .reason(blacklist.getReason())
                .dangerLevel(blacklist.getDangerLevel())
                .createdAt(blacklist.getCreatedAt())
                .expiredAt(blacklist.getExpiredAt())
                .build();
    }

}
