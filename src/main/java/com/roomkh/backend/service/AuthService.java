package com.roomkh.backend.service;

import com.roomkh.backend.dto.auth.LoginRequest;
import com.roomkh.backend.dto.auth.RegisterRequest;

public interface AuthService {
    AuthenticationResult register(RegisterRequest request);
    AuthenticationResult login(LoginRequest request, String clientIp);
    RefreshResult refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
}