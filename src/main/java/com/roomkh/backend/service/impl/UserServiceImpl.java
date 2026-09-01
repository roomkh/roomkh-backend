package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.user.UpdateProfileRequest;
import com.roomkh.backend.dto.user.UserProfileResponse;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(Long userId) {
        User user = findUserById(userId);
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        user.setFullName(request.getFullName().trim());

        // Check and Update Email
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (user.getEmail() != null && !user.getEmail().equalsIgnoreCase(newEmail)) {
                throw new BadRequestException("You cannot change your email address through this profile update. Please use the specific change-email feature.");
            }
            if (user.getEmail() == null) {
                if (userRepository.findByEmailIgnoreCase(newEmail).isPresent()) {
                    throw new BadRequestException("This email is already in use by another account.");
                }
                user.setEmail(newEmail);
            }
        }

        // Check and Update Phone Number
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String newPhone = request.getPhoneNumber().trim();
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().equals(newPhone)) {
                throw new BadRequestException("You cannot change your phone number through this profile update. Please use the specific change-phone feature.");
            }
            if (user.getPhoneNumber() == null) {
                if (userRepository.findByPhoneNumber(newPhone).isPresent()) {
                    throw new BadRequestException("This phone number is already in use by another account.");
                }
                user.setPhoneNumber(newPhone);
            }
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = findUserById(userId);

        if (file.isEmpty()) {
            throw new BadRequestException("Please select a file to upload.");
        }

        try {
            Path uploadPath = Paths.get("uploads/avatars");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = "user_" + userId + "_" + System.currentTimeMillis() + fileExtension;

            java.nio.file.Path filePath = uploadPath.resolve(newFileName);
            java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = "/images/avatars/" + newFileName;

            user.setAvatarUrl(avatarUrl);
            User updatedUser = userRepository.save(user);

            return mapToResponse(updatedUser);

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to store avatar image.", e);
        }
    }

    // New Helper method to find User by ID
    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private UserProfileResponse mapToResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().getName().name() : null)
                .sellerStatus(user.getSellerStatus() != null ? user.getSellerStatus().name() : null)
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .joinedAt(user.getCreatedAt())
                .build();
    }
}