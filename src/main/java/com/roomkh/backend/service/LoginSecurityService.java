package com.roomkh.backend.service;

public interface LoginSecurityService {
    void assertNotBlocked(String rawIp, String rawIdentifier);
    void recordFailedAttempt(String rawIp, String rawIdentifier);
    void recordSuccessfulLogin(String rawIp, String rawIdentifier);
}