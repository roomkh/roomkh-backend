package tech.techey.property.auth.dto;

import tech.techey.property.user.dto.UserResponse;

/**
 * Issued on register/login/refresh. The BFF (Next.js) is expected to keep {@code refreshToken}
 * server-side in an httpOnly cookie and forward only {@code accessToken}/{@code user} to the
 * browser (architecture doc §4.3) — but Spring itself is transport-agnostic and returns both.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserResponse user) {
    @Override public String toString() {
        return "TokenResponse[credentials=REDACTED]";
    }
}
