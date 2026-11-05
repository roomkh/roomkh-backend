package tech.techey.property.user.dto;

import java.time.Instant;
import java.util.UUID;

/** Public-safe projection of {@link tech.techey.property.user.User} — never expose the entity directly. */
public record UserResponse(
        Long id,
        UUID uuid,
        String fullName,
        String email,
        String phone,
        boolean phoneVerified,
        String avatarUrl,
        String preferredLanguage,
        String role,
        String status,
        Instant lastLoginAt,
        Instant createdAt) {
}
