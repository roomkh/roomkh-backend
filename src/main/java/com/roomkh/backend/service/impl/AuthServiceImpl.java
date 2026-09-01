package com.roomkh.backend.service.impl;

import com.roomkh.backend.config.JwtProperties;
import com.roomkh.backend.config.RefreshTokenProperties;
import com.roomkh.backend.dto.auth.*;
import com.roomkh.backend.entity.*;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.DuplicateResourceException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.mapper.UserMapper;
import com.roomkh.backend.repository.PasswordResetOtpRepository;
import com.roomkh.backend.repository.RoleRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.security.JwtService;
import com.roomkh.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern CAMBODIA_LOCAL_PHONE_PATTERN = Pattern.compile("^0\\d{8,9}$");
    private static final Pattern CAMBODIA_E164_PHONE_PATTERN = Pattern.compile("^\\+855\\d{8,9}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenProperties refreshTokenProperties;
    private final LoginSecurityService loginSecurityService;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = resolveUserForLogin(request.getIdentifier());

        if (user.getRole().getName() == RoleName.ADMIN) {
            throw new AccessDeniedException("Administrators cannot reset passwords via this portal. Please contact System Support.");
        }

        String otpCode = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));

        PasswordResetOtp otp = PasswordResetOtp.builder()
                .user(user)
                .otpCode(otpCode)
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();

        passwordResetOtpRepository.save(otp);

        if (looksLikeEmail(request.getIdentifier())) {
            emailService.sendOtpEmail(user.getEmail(), otpCode);
        } else {
            System.out.println("========== MOCK SMS ==========");
            System.out.println("To Phone: " + user.getPhoneNumber());
            System.out.println("OTP Code: " + otpCode);
            System.out.println("==============================");
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = resolveUserForLogin(request.getIdentifier());

        if (user.getRole().getName() == RoleName.ADMIN) {
            throw new AccessDeniedException("Administrators cannot reset passwords via this portal.");
        }

        PasswordResetOtp otpRecord = passwordResetOtpRepository
                .findTopByUserAndIsUsedFalseOrderByExpiresAtDesc(user)
                .orElseThrow(() -> new BadRequestException("No active OTP found. Please request a new one."));

        if (otpRecord.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        if (!otpRecord.getOtpCode().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP code.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpRecord.setUsed(true);
        passwordResetOtpRepository.save(otpRecord);
    }

    @Override
    @Transactional
    public AuthenticationResult register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new BadRequestException("Password confirmation does not match.");
        }

        String rawIdentifier = request.getIdentifier().trim();
        String email = null;
        String phoneNumber = null;

        if (looksLikeEmail(rawIdentifier)) {
            email = normalizeEmail(rawIdentifier);
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new DuplicateResourceException("Email already exists.");
            }
        } else {
            phoneNumber = normalizeCambodiaPhone(rawIdentifier);
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                throw new DuplicateResourceException("Phone number already exists.");
            }
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default USER role is not configured."));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(email)
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .accountStatus(AccountStatus.ACTIVE)
                .sellerStatus(null)
                .role(userRole)
                .build();

        User savedUser = userRepository.save(user);
        return buildAuthenticationResult(savedUser, false);
    }

    @Override
    public AuthenticationResult login(LoginRequest request, String clientIp) {
        String normalizedIdentifier = safeNormalizeIdentifier(request.getIdentifier());

        loginSecurityService.assertNotBlocked(clientIp, normalizedIdentifier);

        User user = null;
        boolean credentialsValid = false;

        try {
            user = resolveUserForLogin(request.getIdentifier());
            credentialsValid = user.getAuthProvider() == AuthProvider.LOCAL
                    && passwordEncoder.matches(request.getPassword(), user.getPassword());
        } catch (BadCredentialsException ex) {
            credentialsValid = false;
        }

        if (!credentialsValid) {
            loginSecurityService.recordFailedAttempt(clientIp, normalizedIdentifier);
            throw new BadCredentialsException("Invalid credentials.");
        }

        loginSecurityService.recordSuccessfulLogin(clientIp, normalizedIdentifier);

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
        String newAccessToken = jwtService.generateToken(user.getId(), user.getRole().getName());

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
        String accessToken = jwtService.generateToken(user.getId(), user.getRole().getName());
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

    private User resolveUserForLogin(String rawIdentifier) {
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        String identifier = rawIdentifier.trim();

        try {
            if (looksLikeEmail(identifier)) {
                String email = normalizeEmail(identifier);
                return userRepository.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
            } else {
                String phoneNumber = normalizeCambodiaPhone(identifier);
                return userRepository.findByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
            }
        } catch (BadRequestException ex) {
            throw new BadCredentialsException("Invalid credentials.");
        }
    }

    private String safeNormalizeIdentifier(String rawIdentifier) {
        if (rawIdentifier == null) {
            return null;
        }
        String trimmed = rawIdentifier.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            if (looksLikeEmail(trimmed)) {
                return normalizeEmail(trimmed);
            }
            return normalizeCambodiaPhone(trimmed);
        } catch (BadRequestException ex) {
            return trimmed.toLowerCase();
        }
    }

    private boolean looksLikeEmail(String identifier) {
        return identifier.contains("@");
    }

    private String normalizeEmail(String rawEmail) {
        String trimmed = rawEmail.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("Please provide a valid email address.");
        }
        return trimmed;
    }

    private String normalizeCambodiaPhone(String rawPhone) {
        String cleaned = rawPhone.trim().replaceAll("[\\s\\-().]", "");

        if (CAMBODIA_E164_PHONE_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }

        if (CAMBODIA_LOCAL_PHONE_PATTERN.matcher(cleaned).matches()) {
            return "+855" + cleaned.substring(1);
        }

        throw new BadRequestException("Please provide a valid Cambodia phone number.");
    }

    private boolean wasRememberMeToken(RefreshToken existingToken) {
        long hoursUntilExpiry = Duration.between(existingToken.getCreatedAt(), existingToken.getExpiresAt()).toHours();
        return hoursUntilExpiry > refreshTokenProperties.getSessionExpirationHours();
    }
}