package com.roomkh.backend.service;

import com.roomkh.backend.entity.RefreshToken;
import com.roomkh.backend.entity.User;

public interface RefreshTokenService {
    String createRefreshToken(User user, boolean rememberMe);
    RefreshToken validateRefreshToken(String rawToken);
    void revokeRefreshToken(RefreshToken refreshToken);
    void revokeByRawToken(String rawToken);
}