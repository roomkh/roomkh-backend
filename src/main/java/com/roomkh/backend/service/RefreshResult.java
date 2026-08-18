package com.roomkh.backend.service;

import com.roomkh.backend.dto.auth.RefreshTokenResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshResult {
    private final RefreshTokenResponse response;
    private final String rawRefreshToken;
    private final boolean rememberMe;
    private final long refreshTokenMaxAgeSeconds;
}