package com.roomkh.backend.dto.auth;

import com.roomkh.backend.entity.AccountStatus;
import com.roomkh.backend.entity.AuthProvider;
import com.roomkh.backend.entity.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private RoleName role;
    private AuthProvider authProvider;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;
}