package com.roomkh.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class LoginSecurityProperties {
    private int maxFailedAttempts;
    private int failedAttemptWindowMinutes;
    private int blockDurationHours;
    private boolean trustProxyHeaders;
    private String rateLimitHashSecret;
}