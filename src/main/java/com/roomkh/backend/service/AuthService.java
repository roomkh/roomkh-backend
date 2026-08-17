package com.roomkh.backend.service;

import com.roomkh.backend.dto.auth.LoginRequest;
import com.roomkh.backend.dto.auth.LoginResponse;
import com.roomkh.backend.dto.auth.RegisterRequest;
import com.roomkh.backend.dto.auth.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}