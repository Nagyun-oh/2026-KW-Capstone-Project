package com.example.security_log_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LogNotFoundException extends RuntimeException {

    public LogNotFoundException(Long logId) {
        super("Log not found. id=" + logId);
    }
}
