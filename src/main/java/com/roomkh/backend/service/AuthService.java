package com.roomkh.backend.service;

import com.roomkh.backend.dto.auth.ForgotPasswordRequest;
import com.roomkh.backend.dto.auth.LoginRequest;
import com.roomkh.backend.dto.auth.RegisterRequest;
import com.roomkh.backend.dto.auth.ResetPasswordRequest;

public interface AuthService {
    AuthenticationResult register(RegisterRequest request);
    AuthenticationResult login(LoginRequest request, String clientIp);
    RefreshResult refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}