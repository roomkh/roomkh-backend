package com.roomkh.backend.controller;

import com.roomkh.backend.dto.auth.AuthResponse;
import com.roomkh.backend.dto.auth.LoginRequest;
import com.roomkh.backend.dto.auth.RefreshTokenResponse;
import com.roomkh.backend.dto.auth.RegisterRequest;
import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.security.RefreshTokenCookieUtil;
import com.roomkh.backend.service.AuthService;
import com.roomkh.backend.service.AuthenticationResult;
import com.roomkh.backend.service.ClientIpResolver;
import com.roomkh.backend.service.RefreshResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, refresh, and logout")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieUtil cookieUtil;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/forgot-password")
    @Operation(summary = "Request OTP for password reset", description = "Sends a 6-digit OTP to the user's email or phone number.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody com.roomkh.backend.dto.auth.ForgotPasswordRequest request) {
        authService.forgotPassword(request);

        return ResponseEntity.ok(ApiResponse.success("If the account exists, an OTP has been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP", description = "Verifies the OTP and updates the user's password.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody com.roomkh.backend.dto.auth.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully.", null));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletResponse response) {
        AuthenticationResult result = authService.register(request);
        cookieUtil.addRefreshTokenCookie(response, result.getRawRefreshToken(), result.isRememberMe(), result.getRefreshTokenMaxAgeSeconds());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful.", result.getAuthResponse()));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email or phone identifier")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest,
                                                           HttpServletResponse response) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        AuthenticationResult result = authService.login(request, clientIp);
        cookieUtil.addRefreshTokenCookie(response, result.getRawRefreshToken(), result.isRememberMe(), result.getRefreshTokenMaxAgeSeconds());
        return ResponseEntity.ok(ApiResponse.success("Login successful.", result.getAuthResponse()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh the access token using the refresh_token cookie")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            HttpServletResponse response) {

        RefreshResult result = authService.refresh(refreshTokenCookie);
        cookieUtil.addRefreshTokenCookie(response, result.getRawRefreshToken(), result.isRememberMe(), result.getRefreshTokenMaxAgeSeconds());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully.", result.getResponse()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke the refresh token")
    public ResponseEntity<ApiResponse<Object>> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            HttpServletResponse response) {

        authService.logout(refreshTokenCookie);
        cookieUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success("Logout successful.", null));
    }
}