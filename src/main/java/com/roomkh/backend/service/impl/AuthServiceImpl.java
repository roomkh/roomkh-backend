package com.roomkh.backend.service.impl;

import com.roomkh.backend.config.JwtProperties;
import com.roomkh.backend.dto.auth.AuthResponse;
import com.roomkh.backend.dto.auth.LoginRequest;
import com.roomkh.backend.dto.auth.RegisterRequest;
import com.roomkh.backend.entity.AccountStatus;
import com.roomkh.backend.entity.AuthProvider;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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

        String accessToken = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().getName());

        return AuthResponse.builder()
                .user(userMapper.toUserResponse(savedUser))
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
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

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().getName());

        return AuthResponse.builder()
                .user(userMapper.toUserResponse(user))
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();
    }

    private String normalizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return null;
        }
        String trimmed = rawPhoneNumber.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}