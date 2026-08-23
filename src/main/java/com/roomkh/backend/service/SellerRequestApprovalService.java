package com.roomkh.backend.service;

import com.roomkh.backend.dto.seller.SellerActivationResponse;
import com.roomkh.backend.dto.seller.SellerRequestApprovalResponse;

public interface SellerRequestApprovalService {
    SellerRequestApprovalResponse approve(Long sellerRequestId, String adminNote, Long adminUserId);
    SellerRequestApprovalResponse reject(Long sellerRequestId, String adminNote, Long adminUserId);
    SellerRequestApprovalResponse resendActivationOtp(Long sellerRequestId);
    SellerActivationResponse activate(Long sellerRequestId, String otpCode, String password, String confirmPassword);
}