package com.example.security_log_system.service;

import com.example.security_log_system.entity.IpBlacklist;
import com.example.security_log_system.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Transactional
public class BlacklistService {
    private final BlacklistRepository blacklistRepository;

    public void addToBlacklist(String ip, String reason,int dangerLevel){
        if(blacklistRepository.findByIpAddress(ip).isEmpty()){
            blacklistRepository.save(IpBlacklist.builder()
                    .ipAddress(ip)
                    .reason(reason)
                    .dangerLevel(dangerLevel)
                    .createdAt(LocalDateTime.now())
                    .build());
            System.out.println("[차단 완료] "+ip);
        }else {
            System.out.println("이미 차단된 IP 입니다.");
        }
    }

    public boolean isBlocked(String ip){
        return blacklistRepository.findByIpAddress(ip).isPresent();
    }

}
