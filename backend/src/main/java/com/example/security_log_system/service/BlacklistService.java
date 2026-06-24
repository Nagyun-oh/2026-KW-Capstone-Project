package com.example.security_log_system.service;

import com.example.security_log_system.dto.BlacklistResponseDto;
import com.example.security_log_system.entity.IpBlacklist;
import com.example.security_log_system.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class BlacklistService {
    private final BlacklistRepository blacklistRepository;

    @Transactional(readOnly = true)
    public Page<BlacklistResponseDto> getAllBlacklists(Pageable pageable){
        return blacklistRepository.findAll(pageable)
                .map(BlacklistResponseDto::from);
    }

    // 추가
    public boolean addToBlacklist(String ip, String reason,int dangerLevel){

        // 중복처리
        if(blacklistRepository.findByIpAddress(ip).isPresent()){
            System.out.println("이미 블랙리스트에 등록되었습니다.");
            return false;
        }

        IpBlacklist blacklist = IpBlacklist.builder()
                .ipAddress(ip)
                .reason(reason)
                .dangerLevel(dangerLevel)
                .createdAt(LocalDateTime.now())
                .build();

        blacklistRepository.save(blacklist);
        return true;
    }

    // 삭제
    @Transactional
    public boolean deleteBlacklist(Long id){
        if(!blacklistRepository.existsById(id)){
            return false;
        }

        blacklistRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(String ipAddress){
        return blacklistRepository.findByIpAddress(ipAddress).isPresent();
    }

}

/*
TODO
    - ip, reason, dangerLevel 검증 추가
    - dangerLevel 범위 1~5 같은 정책 명확화
    - expiredAt 설정/만료 처리 로직 추가
    - 이미 등록된 IP일 때 위험도나 사유 업데이트할지 정책 결정
    - Logger 사용
* */