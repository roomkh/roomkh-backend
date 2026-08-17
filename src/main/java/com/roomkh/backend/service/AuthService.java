package com.roomkh.backend.service;

import com.roomkh.backend.dto.auth.RegisterRequest;
import com.roomkh.backend.dto.auth.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
}