package com.roomkh.backend.service;

public interface SmsSender {
    void sendSellerActivationOtp(String phoneNumber, String otpCode);
}