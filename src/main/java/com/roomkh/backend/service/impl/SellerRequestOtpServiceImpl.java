package com.roomkh.backend.service.impl;

import com.roomkh.backend.entity.SellerRequest;
import com.roomkh.backend.entity.SellerRequestOtpCode;
import com.roomkh.backend.entity.SellerRequestOtpPurpose;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.ServiceUnavailableException;
import com.roomkh.backend.exception.TooManyRequestsException;
import com.roomkh.backend.repository.SellerRequestOtpCodeRepository;
import com.roomkh.backend.service.SellerRequestOtpService;
import com.roomkh.backend.service.SmsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerRequestOtpServiceImpl implements SellerRequestOtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS_PER_HOUR = 3;

    private final SellerRequestOtpCodeRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final Optional<SmsSender> smsSender;

    @Override
    @Transactional
    public SellerRequestOtpCode generateAndSendOtp(SellerRequest sellerRequest) {
        return issueOtp(sellerRequest);
    }

    @Override
    @Transactional
    public SellerRequestOtpCode resendOtp(SellerRequest sellerRequest) {
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        long recentSends = otpRepository.countBySellerRequest_IdAndCreatedAtAfter(sellerRequest.getId(), oneHourAgo);

        if (recentSends >= MAX_RESENDS_PER_HOUR) {
            long retryAfter = Duration.between(OffsetDateTime.now(), oneHourAgo.plusHours(1)).getSeconds();
            throw new TooManyRequestsException(
                    "Too many seller request attempts. Please try again later.",
                    Math.max(retryAfter, 0)
            );
        }

        invalidateActiveOtp(sellerRequest.getId());
        return issueOtp(sellerRequest);
    }

    @Override
    @Transactional
    public void verifyOtp(SellerRequest sellerRequest, String rawOtpCode) {
        List<SellerRequestOtpCode> activeOtps = otpRepository.findActiveForUpdate(sellerRequest.getId());

        SellerRequestOtpCode otp = activeOtps.stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP code."));

        OffsetDateTime now = OffsetDateTime.now();

        if (otp.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("OTP code has expired.");
        }

        if (otp.getAttemptCount() >= otp.getMaxAttempts()) {
            long retryAfter = Duration.between(now, otp.getExpiresAt()).getSeconds();
            throw new TooManyRequestsException("Maximum OTP verification attempts reached.", Math.max(retryAfter, 0));
        }

        if (!passwordEncoder.matches(rawOtpCode, otp.getCodeHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
            throw new BadRequestException("Invalid OTP code.");
        }

        otp.setConsumedAt(now);
        otpRepository.save(otp);
    }

    private void invalidateActiveOtp(Long sellerRequestId) {
        List<SellerRequestOtpCode> activeOtps = otpRepository.findActiveForUpdate(sellerRequestId);
        OffsetDateTime now = OffsetDateTime.now();
        activeOtps.forEach(otp -> otp.setInvalidatedAt(now));
        otpRepository.saveAll(activeOtps);
    }

    private SellerRequestOtpCode issueOtp(SellerRequest sellerRequest) {
        if (smsSender.isEmpty()) {
            throw new ServiceUnavailableException("SMS delivery is not configured.");
        }

        String rawOtp = generateNumericOtp();
        String codeHash = passwordEncoder.encode(rawOtp);
        OffsetDateTime now = OffsetDateTime.now();

        SellerRequestOtpCode otp = SellerRequestOtpCode.builder()
                .sellerRequest(sellerRequest)
                .codeHash(codeHash)
                .purpose(SellerRequestOtpPurpose.SELLER_ACTIVATION)
                .attemptCount(0)
                .maxAttempts(OTP_MAX_ATTEMPTS)
                .expiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        SellerRequestOtpCode saved = otpRepository.save(otp);

        smsSender.get().sendSellerActivationOtp(sellerRequest.getPhoneNumber(), rawOtp);

        return saved;
    }

    private String generateNumericOtp() {
        int otpValue = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", otpValue);
    }
}