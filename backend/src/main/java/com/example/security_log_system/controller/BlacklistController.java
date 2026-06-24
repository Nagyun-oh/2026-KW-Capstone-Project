package com.example.security_log_system.controller;

import com.example.security_log_system.dto.BlacklistRequest;
import com.example.security_log_system.dto.BlacklistResponseDto;
import com.example.security_log_system.entity.IpBlacklist;
import com.example.security_log_system.service.BlacklistService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "BlackList API", description = "차단 데이터 조회 및 관리 API")
@Validated
@RestController
@RequestMapping("/api/v1/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    // GET (createdAt 기준 최신순으로 20개씩 반환)
    @GetMapping
    public ResponseEntity<Page<BlacklistResponseDto>>getBlackLists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC,"createdAt")
        );

        return ResponseEntity.ok(blacklistService.getAllBlacklists(pageable));
    }

    // POST
    @PostMapping
    public ResponseEntity<String> addBlackList(@Valid @RequestBody BlacklistRequest request){

        boolean added = blacklistService.addToBlacklist(
                request.getIpAddress(),
                request.getReason(),
                request.getDangerLevel()
        );

        if(!added){
            return ResponseEntity.badRequest()
                    .body("IP is already blacklisted.");
        }

        return ResponseEntity.ok(
                "IP was added to the blacklist."
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlacklist(@PathVariable Long id){
        //존재하지 않음 → 404 Not Found
        if(!blacklistService.deleteBlacklist(id)){
            return ResponseEntity.notFound().build();
        }
        // 삭제 성공 → 204 No Content
        return ResponseEntity.noContent().build();
    }

}

/*
TODO
    - 중복 등록 시 단순 문자열보다 에러 응답 DTO 사용
    - 삭제 API DELETE /api/v1/blacklist/{id} 또는 /ip/{ip} 추가 고려
*/