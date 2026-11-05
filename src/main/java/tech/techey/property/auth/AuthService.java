package tech.techey.property.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.techey.property.auth.dto.LoginRequest;
import tech.techey.property.auth.dto.RegisterRequest;
import tech.techey.property.auth.dto.TokenResponse;
import tech.techey.property.common.exception.ApiException;
import tech.techey.property.common.exception.ErrorCode;
import tech.techey.property.common.security.JwtService;
import tech.techey.property.user.Role;
import tech.techey.property.user.AuthProvider;
import tech.techey.property.user.User;
import tech.techey.property.user.UserRepository;
import tech.techey.property.user.UserService;
import tech.techey.property.user.UserStatus;

/**
 * Registration, login and refresh-token rotation.
 *
 * <p>Refresh tokens are opaque, stored hashed, and grouped by {@code familyId}
 * (architecture doc §3.4). Each successful {@link #refresh} call revokes the token
 * just used and issues a new one in the same family ("rotation"). If a token that
 * was already revoked is presented again — a stolen/replayed token — the entire
 * family is revoked, forcing re-login on every device sharing that session chain.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 30;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenFamilyRevoker refreshTokenFamilyRevoker;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.of(ErrorCode.CONFLICT, "email");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.accountType() == null ? Role.USER : Role.valueOf(request.accountType()))
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        return issueNewSession(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.of(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.of(ErrorCode.FORBIDDEN);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueNewSession(user);
    }

    public TokenResponse googleLogin(GoogleIdentity identity, String portal) {
        User user = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, identity.subject())
                .orElse(null);
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(identity.email()).orElse(null);
            if (user != null) {
                // Google is not authoritative for third-party email addresses. Never
                // silently link those, or replace a different provider/Google subject.
                if (user.getProvider() != AuthProvider.LOCAL || !identity.authoritativeEmail()) {
                    throw ApiException.of(ErrorCode.GOOGLE_ACCOUNT_CONFLICT);
                }
            } else {
                if ("ADMIN".equals(portal)) throw ApiException.of(ErrorCode.FORBIDDEN);
                user = User.builder().email(identity.email()).fullName(identity.name())
                        .avatarUrl(identity.picture()).role(Role.OWNER).status(UserStatus.ACTIVE).build();
            }
        }
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null
                || ("ADMIN".equals(portal) && user.getRole() != Role.ADMIN)
                || ("OWNER".equals(portal) && user.getRole() != Role.OWNER
                    && user.getRole() != Role.AGENT && user.getRole() != Role.ADMIN)) {
            throw ApiException.of(ErrorCode.FORBIDDEN);
        }
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(identity.subject());
        user.setLastLoginAt(Instant.now());
        user = userRepository.save(user);
        return issueNewSession(user);
    }

    public TokenResponse refresh(String rawRefreshToken) {
        String hash = jwtService.hashToken(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.of(ErrorCode.UNAUTHORIZED));

        if (existing.isRevoked()) {
            // Reuse of an already-rotated token: treat as a stolen token and burn the
            // whole family so every descendant session is forced to re-authenticate.
            // Committed in its own transaction (see RefreshTokenFamilyRevoker) so the
            // revocation survives the ApiException thrown right after, which would
            // otherwise roll back this method's transaction and undo it.
            log.warn("Refresh token reuse detected for family {}", existing.getFamilyId());
            refreshTokenFamilyRevoker.revokeFamily(existing.getFamilyId());
            throw ApiException.of(ErrorCode.TOKEN_REUSE_DETECTED);
        }
        if (existing.isExpired()) {
            throw ApiException.of(ErrorCode.TOKEN_EXPIRED);
        }

        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);

        User user = existing.getUser();
        return issueRotatedSession(user, existing.getFamilyId());
    }

    public void logout(String rawRefreshToken) {
        String hash = jwtService.hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    private TokenResponse issueNewSession(User user) {
        return issueRotatedSession(user, UUID.randomUUID());
    }

    private TokenResponse issueRotatedSession(User user, UUID familyId) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateOpaqueRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(rawRefreshToken))
                .familyId(familyId)
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenTtlSeconds(),
                userService.toResponse(user));
    }
}
