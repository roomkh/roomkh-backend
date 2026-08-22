package com.roomkh.backend.service.impl;

import com.roomkh.backend.config.SellerRequestRateLimitProperties;
import com.roomkh.backend.entity.SellerRequestRateLimitKeyType;
import com.roomkh.backend.entity.SellerRequestRateLimitRecord;
import com.roomkh.backend.entity.SellerRequestRateLimitWindowType;
import com.roomkh.backend.exception.TooManyRequestsException;
import com.roomkh.backend.repository.SellerRequestRateLimitRecordRepository;
import com.roomkh.backend.service.SellerRequestRateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
public class SellerRequestRateLimitServiceImpl implements SellerRequestRateLimitService {

    private final SellerRequestRateLimitRecordRepository repository;
    private final SellerRequestRateLimitProperties properties;

    @Override
    @Transactional(noRollbackFor = TooManyRequestsException.class)
    public void checkAndRecordAttempt(String clientIp, Long authenticatedUserId) {
        if (clientIp != null && !clientIp.isBlank()) {
            enforceIpHardFloodLimit(clientIp);
            enforceIpSoftMinuteLimit(clientIp);
        }

        if (authenticatedUserId != null) {
            enforceUserDailyLimit(authenticatedUserId);
        }
    }

    private void enforceIpHardFloodLimit(String rawIp) {
        RateLimitOutcome outcome = checkAndIncrement(
                SellerRequestRateLimitKeyType.IP,
                rawIp,
                SellerRequestRateLimitWindowType.SECOND,
                Duration.ofSeconds(1),
                properties.getHardFloodRequestsPerSecond(),
                true
        );
        rejectIfExceeded(outcome);
    }

    private void enforceIpSoftMinuteLimit(String rawIp) {
        RateLimitOutcome outcome = checkAndIncrement(
                SellerRequestRateLimitKeyType.IP,
                rawIp,
                SellerRequestRateLimitWindowType.MINUTE,
                Duration.ofMinutes(1),
                properties.getMaxRequestsPerMinute(),
                false
        );
        rejectIfExceeded(outcome);
    }

    private void enforceUserDailyLimit(Long userId) {
        RateLimitOutcome outcome = checkAndIncrement(
                SellerRequestRateLimitKeyType.USER,
                String.valueOf(userId),
                SellerRequestRateLimitWindowType.DAY,
                Duration.ofDays(1),
                properties.getMaxRequestsPerUserPerDay(),
                false
        );
        rejectIfExceeded(outcome);
    }

    private void rejectIfExceeded(RateLimitOutcome outcome) {
        if (outcome.limitExceeded()) {
            throw new TooManyRequestsException(
                    "Too many seller request attempts. Please try again later.",
                    outcome.retryAfterSeconds()
            );
        }
    }

    private RateLimitOutcome checkAndIncrement(SellerRequestRateLimitKeyType keyType, String rawValue,
                                                SellerRequestRateLimitWindowType windowType, Duration windowDuration,
                                                int maxRequests, boolean blockOnExceed) {
        String hash = hash(rawValue);
        OffsetDateTime now = OffsetDateTime.now();

        SellerRequestRateLimitRecord record = repository
                .findForUpdate(keyType, hash, windowType)
                .orElseGet(() -> createNewRecord(keyType, hash, windowType, now));

        boolean currentlyBlocked = record.getBlockedUntil() != null && record.getBlockedUntil().isAfter(now);
        if (currentlyBlocked) {
            long retrySeconds = Duration.between(now, record.getBlockedUntil()).getSeconds();
            return new RateLimitOutcome(true, Math.max(retrySeconds, 0));
        }

        boolean windowExpired = record.getWindowStartedAt().isBefore(now.minus(windowDuration));

        if (windowExpired) {
            record.setRequestCount(1);
            record.setWindowStartedAt(now);
        } else {
            record.setRequestCount(record.getRequestCount() + 1);
        }

        boolean limitExceeded = record.getRequestCount() > maxRequests;

        if (limitExceeded && blockOnExceed) {
            record.setBlockedUntil(now.plusHours(properties.getIpBlockDurationHours()));
        }

        repository.save(record);

        if (!limitExceeded) {
            return new RateLimitOutcome(false, 0);
        }

        long retrySeconds = record.getBlockedUntil() != null
                ? Duration.between(now, record.getBlockedUntil()).getSeconds()
                : Duration.between(now, record.getWindowStartedAt().plus(windowDuration)).getSeconds();

        return new RateLimitOutcome(true, Math.max(retrySeconds, 0));
    }

    private SellerRequestRateLimitRecord createNewRecord(SellerRequestRateLimitKeyType keyType, String hash,
                                                           SellerRequestRateLimitWindowType windowType, OffsetDateTime now) {
        try {
            SellerRequestRateLimitRecord newRecord = SellerRequestRateLimitRecord.builder()
                    .keyType(keyType)
                    .keyHash(hash)
                    .windowType(windowType)
                    .requestCount(0)
                    .windowStartedAt(now)
                    .build();
            return repository.saveAndFlush(newRecord);
        } catch (DataIntegrityViolationException ex) {
            return repository.findForUpdate(keyType, hash, windowType)
                    .orElseThrow(() -> ex);
        }
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

    private record RateLimitOutcome(boolean limitExceeded, long retryAfterSeconds) {
    }
}