package com.roomkh.backend.service;

import com.roomkh.backend.dto.auth.AuthResponse;
import com.roomkh.backend.dto.auth.LoginRequest;
import com.roomkh.backend.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}