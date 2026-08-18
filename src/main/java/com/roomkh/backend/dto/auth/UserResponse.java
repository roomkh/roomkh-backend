package com.roomkh.backend.dto.auth;

import com.roomkh.backend.entity.AuthProvider;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.entity.SellerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private RoleName role;
    private SellerStatus sellerStatus;
    private AuthProvider authProvider;
}