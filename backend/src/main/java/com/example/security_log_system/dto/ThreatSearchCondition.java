package com.example.security_log_system.dto;


import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ThreatSearchCondition {

    private String threatType;

    @Pattern(
            regexp = "(?i)MEDIUM|HIGH|CRITICAL",
            message = "Invalid severity."
    )

    private String severity;
}
