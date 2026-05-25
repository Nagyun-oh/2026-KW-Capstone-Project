package com.example.security_log_system.controller;

import com.example.security_log_system.dto.BlacklistRequest;
import com.example.security_log_system.entity.IpBlacklist;
import com.example.security_log_system.repository.BlacklistRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

//위협이 확인된 IP를 차단 목록에 넣고 빼는 기능 담당
@Tag(name = "BlackList API", description = "차단 데이터 조회 및 관리 API")
@RestController
@RequestMapping("api/v1/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistRepository blacklistRepository;

    // 현재 차단된 모든 IP 목록 조회
    @GetMapping
    public ResponseEntity<List<IpBlacklist>>getBlackList(){
        return ResponseEntity.ok(blacklistRepository.findAll());
    }

    // 새로운 IP 차단 등록
    @PostMapping
    public ResponseEntity<String> addBlackList(@RequestBody BlacklistRequest request){

        // 중복 체크 로직 (예시)
        if(blacklistRepository.findByIpAddress(request.getIpAddress()).isPresent()){
            return ResponseEntity.badRequest().body("이미 차단된 IP입니다.");
        }

        // 객체 생성
        IpBlacklist blacklist = IpBlacklist.builder()
                .ipAddress(request.getIpAddress())
                .reason(request.getReason())
                .dangerLevel(request.getDangerlevel())
                .createdAt(LocalDateTime.now())
                .build();

        blacklistRepository.save(blacklist);

        return ResponseEntity.ok("IP " + request.getIpAddress() + "가 블랙리스트에 등록되었습니다.");
    }

}
