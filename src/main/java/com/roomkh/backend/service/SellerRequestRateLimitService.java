package com.roomkh.backend.service;

public interface SellerRequestRateLimitService {
    void checkAndRecordAttempt(String clientIp, Long authenticatedUserId);
}