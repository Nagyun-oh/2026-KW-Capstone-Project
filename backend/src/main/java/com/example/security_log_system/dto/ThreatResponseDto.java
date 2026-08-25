package com.example.security_log_system.dto;

import com.example.security_log_system.entity.DetectedThreat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ThreatResponseDto {

    private Long id;
    private Long logId;
    private String threatType;
    private String detectionSource;
    private Double threatScore;
    private String severity;
    private String description;
    private boolean checked;
    private LocalDateTime detectedAt;

    public static ThreatResponseDto from(DetectedThreat threat){
        return ThreatResponseDto.builder()
                .id(threat.getId())
                .logId(threat.getLogEntry() !=null
                    ? threat.getLogEntry().getId() : null)
                .threatType(threat.getThreatType())
                .detectionSource(threat.getDetectionSource())
                .threatScore(threat.getThreatScore())
                .severity(threat.getSeverity())
                .description(threat.getDescription())
                .checked(threat.isChecked())
                .detectedAt(threat.getDetectedAt())
                .build();
    }
}
