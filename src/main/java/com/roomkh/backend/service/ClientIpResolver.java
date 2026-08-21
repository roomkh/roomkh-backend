package com.roomkh.backend.service;

import com.roomkh.backend.config.LoginSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final LoginSecurityProperties loginSecurityProperties;

    public String resolveClientIp(HttpServletRequest request) {
        if (loginSecurityProperties.isTrustProxyHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}