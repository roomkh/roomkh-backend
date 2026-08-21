package com.roomkh.backend.service.impl;

import com.roomkh.backend.config.LoginSecurityProperties;
import com.roomkh.backend.entity.LoginSecurityKeyType;
import com.roomkh.backend.entity.LoginSecurityRecord;
import com.roomkh.backend.exception.TooManyRequestsException;
import com.roomkh.backend.repository.LoginSecurityRecordRepository;
import com.roomkh.backend.service.LoginSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private final LoginSecurityRecordRepository repository;
    private final LoginSecurityProperties properties;

    @Override
    public void assertNotBlocked(String rawIp, String rawIdentifier) {
        long retryAfterSeconds = Math.max(
                remainingBlockSeconds(LoginSecurityKeyType.IP, rawIp),
                remainingBlockSeconds(LoginSecurityKeyType.IDENTIFIER, rawIdentifier)
        );

        if (retryAfterSeconds > 0) {
            throw new TooManyRequestsException("Too many login attempts. Please try again later.", retryAfterSeconds);
        }
    }

    @Override
    @Transactional
    public void recordFailedAttempt(String rawIp, String rawIdentifier) {
        if (rawIp != null && !rawIp.isBlank()) {
            recordFailure(LoginSecurityKeyType.IP, rawIp);
        }
        if (rawIdentifier != null && !rawIdentifier.isBlank()) {
            recordFailure(LoginSecurityKeyType.IDENTIFIER, rawIdentifier);
        }
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(String rawIp, String rawIdentifier) {
        deleteIfExists(LoginSecurityKeyType.IP, rawIp);
        deleteIfExists(LoginSecurityKeyType.IDENTIFIER, rawIdentifier);
    }

    private long remainingBlockSeconds(LoginSecurityKeyType keyType, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0L;
        }

        String hash = hash(rawValue);
        return repository.findByKeyTypeAndKeyHash(keyType, hash)
                .map(record -> {
                    if (record.getBlockedUntil() == null) {
                        return 0L;
                    }
                    long seconds = Duration.between(OffsetDateTime.now(), record.getBlockedUntil()).getSeconds();
                    return Math.max(seconds, 0L);
                })
                .orElse(0L);
    }

    private void recordFailure(LoginSecurityKeyType keyType, String rawValue) {
        String hash = hash(rawValue);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowStart = now.minusMinutes(properties.getFailedAttemptWindowMinutes());

        LoginSecurityRecord record = repository.findByKeyTypeAndKeyHash(keyType, hash)
                .orElseGet(() -> LoginSecurityRecord.builder()
                        .keyType(keyType)
                        .keyHash(hash)
                        .failedAttempts(0)
                        .windowStartedAt(now)
                        .build());

        boolean windowExpired = record.getWindowStartedAt() == null || record.getWindowStartedAt().isBefore(windowStart);
        boolean currentlyBlocked = record.getBlockedUntil() != null && record.getBlockedUntil().isAfter(now);

        if (windowExpired && !currentlyBlocked) {
            record.setFailedAttempts(1);
            record.setWindowStartedAt(now);
        } else {
            record.setFailedAttempts(record.getFailedAttempts() + 1);
        }

        record.setLastFailedAt(now);

        if (record.getFailedAttempts() >= properties.getMaxFailedAttempts()) {
            record.setBlockedUntil(now.plusHours(properties.getBlockDurationHours()));
        }

        repository.save(record);
    }

    private void deleteIfExists(LoginSecurityKeyType keyType, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }
        String hash = hash(rawValue);
        repository.findByKeyTypeAndKeyHash(keyType, hash).ifPresent(repository::delete);
    }

    private String hash(String rawValue) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    properties.getRateLimitHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(rawValue.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to hash rate limit key.", e);
        }
    }
}