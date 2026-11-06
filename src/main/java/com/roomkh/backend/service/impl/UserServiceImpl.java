package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.user.ChangePasswordRequest;
import com.roomkh.backend.dto.user.UpdateProfileRequest;
import com.roomkh.backend.dto.user.UserProfileResponse;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final java.util.Map<String, String> AVATAR_EXTENSIONS = java.util.Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final java.util.regex.Pattern CAMBODIA_LOCAL_PHONE = java.util.regex.Pattern.compile("^0\\d{8,9}$");
    private static final java.util.regex.Pattern CAMBODIA_E164_PHONE = java.util.regex.Pattern.compile("^\\+855\\d{8,9}$");

    /** "012 345 678" -> "+85512345678"; same rules as AuthServiceImpl. */
    private static String normalizeCambodiaPhone(String rawPhone) {
        String cleaned = rawPhone.trim().replaceAll("[\\s\\-().]", "");
        if (CAMBODIA_E164_PHONE.matcher(cleaned).matches()) {
            return cleaned;
        }
        if (CAMBODIA_LOCAL_PHONE.matcher(cleaned).matches()) {
            return "+855" + cleaned.substring(1);
        }
        throw new BadRequestException("Please provide a valid Cambodia phone number.");
    }

    /** Avatars saved before the fix point at /images/avatars/, which is not served. */
    private static String fixLegacyAvatarUrl(String url) {
        return url != null && url.startsWith("/images/avatars/")
                ? "/uploads/avatars/" + url.substring("/images/avatars/".length())
                : url;
    }

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
            // Same format as registration/login (+855...), otherwise the number can't
            // be used to sign in and the uniqueness check misses "012..." vs "+85512...".
            String newPhone = normalizeCambodiaPhone(request.getPhoneNumber());
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

        // Only real images: the file is served back from /uploads, so accepting
        // any type (e.g. .html) would let users host arbitrary content on our domain.
        String extension = AVATAR_EXTENSIONS.get(
                file.getContentType() == null ? "" : file.getContentType().toLowerCase());
        if (extension == null) {
            throw new BadRequestException("Profile photos must be JPG, PNG or WebP images.");
        }

        try {
            Path uploadPath = Paths.get("uploads/avatars");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String newFileName = "user_" + userId + "_" + System.currentTimeMillis() + extension;

            java.nio.file.Path filePath = uploadPath.resolve(newFileName);
            java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Served by LocalPropertyImageResourceConfig under /uploads/** (it used
            // to return /images/avatars/..., a path nothing serves).
            String avatarUrl = "/uploads/avatars/" + newFileName;

            user.setAvatarUrl(avatarUrl);
            User updatedUser = userRepository.save(user);

            return mapToResponse(updatedUser);

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to store avatar image.", e);
        }
    }

    // New Helper method to find User by ID
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match.");
        }

        User user = findUserById(userId);

        // Google sign-in accounts may have no usable password yet.
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BadRequestException("This account signs in with Google. Use \"Forgot password\" to set a password.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("The new password must be different from the current one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

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
                .avatarUrl(fixLegacyAvatarUrl(user.getAvatarUrl()))
                .role(user.getRole() != null ? user.getRole().getName().name() : null)
                .sellerStatus(user.getSellerStatus() != null ? user.getSellerStatus().name() : null)
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .joinedAt(user.getCreatedAt())
                .build();
    }
}