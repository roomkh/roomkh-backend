package com.roomkh.backend.security;

import com.roomkh.backend.config.CookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieUtil {

    private static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final CookieProperties cookieProperties;

    public void addRefreshTokenCookie(HttpServletResponse response, String rawToken,
                                       boolean rememberMe, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(COOKIE_PATH)
                .sameSite(cookieProperties.getSameSite());

        if (rememberMe) {
            builder.maxAge(Duration.ofSeconds(maxAgeSeconds));
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(COOKIE_PATH)
                .sameSite(cookieProperties.getSameSite())
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}