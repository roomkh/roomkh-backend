package com.roomkh.backend.service.impl;

import com.roomkh.backend.config.JwtProperties;
import com.roomkh.backend.config.RefreshTokenProperties;
import com.roomkh.backend.dto.auth.AuthResponse;
import com.roomkh.backend.dto.auth.LoginRequest;
import com.roomkh.backend.dto.auth.RefreshTokenResponse;
import com.roomkh.backend.dto.auth.RegisterRequest;
import com.roomkh.backend.entity.AccountStatus;
import com.roomkh.backend.entity.AuthProvider;
import com.roomkh.backend.entity.RefreshToken;
import com.roomkh.backend.entity.Role;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.DuplicateResourceException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.mapper.UserMapper;
import com.roomkh.backend.repository.RoleRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.security.JwtService;
import com.roomkh.backend.service.AuthService;
import com.roomkh.backend.service.AuthenticationResult;
import com.roomkh.backend.service.RefreshResult;
import com.roomkh.backend.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenProperties refreshTokenProperties;

    @Override
    @Transactional
    public AuthenticationResult register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new BadRequestException("Password confirmation does not match.");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists.");
        }

        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());

        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("Phone number already exists.");
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default USER role is not configured."));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(phoneNumber)
                .authProvider(AuthProvider.LOCAL)
                .accountStatus(AccountStatus.ACTIVE)
                .sellerStatus(null)
                .role(userRole)
                .build();

        User savedUser = userRepository.save(user);
        return buildAuthenticationResult(savedUser, false);
    }

    @Override
    public AuthenticationResult login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new AccessDeniedException("This account is inactive.");
        }

        return buildAuthenticationResult(user, request.isRememberMe());
    }

    @Override
    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is missing.");
        }

        RefreshToken existingToken = refreshTokenService.validateRefreshToken(rawRefreshToken);
        User user = existingToken.getUser();

        refreshTokenService.revokeRefreshToken(existingToken);

        boolean rememberMe = wasRememberMeToken(existingToken);
        String newRawRefreshToken = refreshTokenService.createRefreshToken(user, rememberMe);
        String newAccessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().getName());

        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();

        long maxAgeSeconds = rememberMe ? refreshTokenProperties.getExpirationDays() * 86400L : 0L;

        return new RefreshResult(response, newRawRefreshToken, rememberMe, maxAgeSeconds);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeByRawToken(rawRefreshToken);
        }
    }

    private AuthenticationResult buildAuthenticationResult(User user, boolean rememberMe) {
        String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().getName());
        String rawRefreshToken = refreshTokenService.createRefreshToken(user, rememberMe);

        AuthResponse authResponse = AuthResponse.builder()
                .user(userMapper.toUserResponse(user))
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();

        long maxAgeSeconds = rememberMe ? refreshTokenProperties.getExpirationDays() * 86400L : 0L;

        return new AuthenticationResult(authResponse, rawRefreshToken, rememberMe, maxAgeSeconds);
    }

    private boolean wasRememberMeToken(RefreshToken existingToken) {
        long hoursUntilExpiry = Duration.between(existingToken.getCreatedAt(), existingToken.getExpiresAt()).toHours();
        return hoursUntilExpiry > refreshTokenProperties.getSessionExpirationHours();
    }

    private String normalizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return null;
        }
        String trimmed = rawPhoneNumber.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}