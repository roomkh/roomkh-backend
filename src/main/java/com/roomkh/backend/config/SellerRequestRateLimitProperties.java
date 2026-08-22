package com.roomkh.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.seller-request-rate-limit")
public class SellerRequestRateLimitProperties {
    private int maxRequestsPerMinute;
    private int hardFloodRequestsPerSecond;
    private int maxRequestsPerUserPerDay;
    private int ipBlockDurationHours;
    private String rateLimitHashSecret;
}