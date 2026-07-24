package com.example.security_log_system.dto;

import com.example.security_log_system.validation.ValidIpAddress;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlacklistSearchCondition {

    @ValidIpAddress
    private String ip;

    @Min(value=1,message= "Danger level must be at least 1.")
    @Max(value=5,message= "Danger level must not exceed 5.")
    private Integer dangerLevel;
}
