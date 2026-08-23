package com.roomkh.backend.service;

import com.roomkh.backend.entity.SellerRequest;
import com.roomkh.backend.entity.SellerRequestOtpCode;

public interface SellerRequestOtpService {
    SellerRequestOtpCode generateAndSendOtp(SellerRequest sellerRequest);
    SellerRequestOtpCode resendOtp(SellerRequest sellerRequest);
    void verifyOtp(SellerRequest sellerRequest, String rawOtpCode);
}