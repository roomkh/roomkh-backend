package tech.techey.property.auth;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes a whole refresh-token family in its own, independent transaction.
 *
 * <p>{@link AuthService#refresh} revokes the family and then throws
 * {@link tech.techey.property.common.exception.ApiException} on reuse detection.
 * If the revocation happened in the same transaction as that throw, Spring's
 * default rollback-on-RuntimeException behavior would undo the revocation —
 * defeating the entire point of reuse detection. {@code REQUIRES_NEW} commits
 * the revocation immediately, independent of whatever the caller does next.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenFamilyRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(UUID familyId) {
        refreshTokenRepository.revokeFamily(familyId, Instant.now());
    }
}
