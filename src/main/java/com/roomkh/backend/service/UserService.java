package com.roomkh.backend.service;

import com.roomkh.backend.dto.user.UpdateProfileRequest;
import com.roomkh.backend.dto.user.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserProfileResponse getCurrentUserProfile(Long userId);
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    UserProfileResponse uploadAvatar(Long userId, MultipartFile file);
}