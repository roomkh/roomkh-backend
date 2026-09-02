package com.roomkh.backend.service;

import com.roomkh.backend.dto.auth.*;

public interface AuthService {
    AuthenticationResult register(RegisterRequest request);
    AuthenticationResult login(LoginRequest request, String clientIp);
    RefreshResult refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);

    AuthenticationResult googleLogin(GoogleLoginRequest request, String clientIp);
}