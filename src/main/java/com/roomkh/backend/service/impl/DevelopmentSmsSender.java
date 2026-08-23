package com.roomkh.backend.service.impl;

import com.roomkh.backend.service.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "development")
public class DevelopmentSmsSender implements SmsSender {

    @Override
    public void sendSellerActivationOtp(String phoneNumber, String otpCode) {
        log.info("SELLER_ACTIVATION_OTP phone={} code={}", maskPhoneNumber(phoneNumber), otpCode);
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "****";
        }
        String prefix = phoneNumber.substring(0, 4);
        String suffix = phoneNumber.substring(phoneNumber.length() - 3);
        return prefix + "******" + suffix;
    }
}