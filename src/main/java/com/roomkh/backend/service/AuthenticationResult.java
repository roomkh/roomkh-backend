package com.roomkh.backend.service;

import com.roomkh.backend.dto.auth.AuthResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticationResult {
    private final AuthResponse authResponse;
    private final String rawRefreshToken;
    private final boolean rememberMe;
    private final long refreshTokenMaxAgeSeconds;
}