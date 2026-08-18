package com.roomkh.backend.mapper;

import com.roomkh.backend.dto.auth.UserResponse;
import com.roomkh.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .sellerStatus(user.getSellerStatus())
                .authProvider(user.getAuthProvider())
                .build();
    }
}