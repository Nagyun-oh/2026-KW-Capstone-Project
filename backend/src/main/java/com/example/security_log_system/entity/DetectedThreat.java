package com.example.security_log_system.entity;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="detected_threats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DetectedThreat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1:N 관계 설정 (어떤 로그에서 발생했는지)
    @ManyToOne(fetch = FetchType.LAZY)  // 성능을 위해 사용
    @JoinColumn(name="log_id")
    @JsonIgnore
    private LogEntry logEntry;

    @Column(name = "threat_type" , length = 50)
    private String threatType;

    @Column(length = 10)
    private String severity; // HIGH, MEDIUM, LOW

    @Column(name = "detection_source", length = 20)
    private String detectionSource; // AI, WAF, MANUAL

    @Column(name = "threat_score")
    private Double threatScore; // AI 점수가 없는 탐지는 null 허용

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_checked")
    private boolean isChecked;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;
}

/*
TODO
    - detectedAt이 null로 저장될 수 있는 경로 확인
    - severity를 String 대신 Enum으로 관리 검토
    - isChecked 기본값 false 명확화
    - Entity 직접 반환 대신 ThreatResponseDto에서 logId만 노출
*/
