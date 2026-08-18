package com.roomkh.backend.controller;

import com.roomkh.backend.dto.auth.AuthResponse;
import com.roomkh.backend.dto.auth.LoginRequest;
import com.roomkh.backend.dto.auth.RefreshTokenResponse;
import com.roomkh.backend.dto.auth.RegisterRequest;
import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.security.RefreshTokenCookieUtil;
import com.roomkh.backend.service.AuthService;
import com.roomkh.backend.service.AuthenticationResult;
import com.roomkh.backend.service.RefreshResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletResponse response) {
        AuthenticationResult result = authService.login(request);
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