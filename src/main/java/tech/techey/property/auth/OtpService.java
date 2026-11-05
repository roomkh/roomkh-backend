package tech.techey.property.auth;

import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub OTP service: generates a code and logs it instead of sending an SMS.
 * Real SMS/Telegram delivery integration is out of scope for this milestone
 * (Foundation + Auth) — wired up here so the shape is right for a follow-up.
 */
@Slf4j
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Generates a 6-digit OTP and "sends" it (logs it) to the given phone number. */
    public String sendOtp(String phone) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        log.info("[OTP STUB] Would send SMS to {} with code {}", phone, code);
        return code;
    }

    /**
     * Verifies a submitted OTP. Always returns {@code true} in this stub since no
     * codes are actually persisted/sent yet — replace with a Redis-backed lookup
     * once real SMS delivery ships.
     */
    public boolean verifyOtp(String phone, String code) {
        log.info("[OTP STUB] Would verify code {} for {}", code, phone);
        return true;
    }
}
