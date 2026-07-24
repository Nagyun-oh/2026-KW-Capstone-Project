package com.example.security_log_system.dto;

import com.example.security_log_system.validation.ValidIpAddress;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LogSearchCondition {

    @ValidIpAddress
    private String ip;

    @Pattern(
            regexp = "(?i)GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD",
            message = "Invalid HTTP method."
    )
    private String method;

    @Min(value = 100,message ="Status code must be at least 100.")
    @Max(value = 599, message = "Status code must not exceed 599.")
    private Integer statusCode;

}
