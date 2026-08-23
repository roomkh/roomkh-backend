package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.seller.AdminSellerRequestDecisionRequest;
import com.roomkh.backend.dto.seller.SellerRequestApprovalResponse;
import com.roomkh.backend.security.CustomUserDetails;
import com.roomkh.backend.service.SellerRequestApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/seller-requests")
@RequiredArgsConstructor
@Tag(name = "Admin - Seller Request Review", description = "Approve, reject, and manage OTP for seller requests")
public class AdminSellerRequestReviewController {

    private final SellerRequestApprovalService approvalService;

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending seller request")
    public ResponseEntity<ApiResponse<SellerRequestApprovalResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) AdminSellerRequestDecisionRequest request) {

        String adminNote = request != null ? request.getAdminNote() : null;
        Long adminUserId = resolveAdminUserId();
        SellerRequestApprovalResponse response = approvalService.approve(id, adminNote, adminUserId);

        String message = response.getUserId() != null
                ? "Seller request approved. User has been promoted to SELLER."
                : "Seller request approved. Activation OTP has been sent.";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending seller request")
    public ResponseEntity<ApiResponse<SellerRequestApprovalResponse>> reject(
            @PathVariable Long id,
            @RequestBody AdminSellerRequestDecisionRequest request) {

        Long adminUserId = resolveAdminUserId();
        SellerRequestApprovalResponse response = approvalService.reject(id, request.getAdminNote(), adminUserId);
        return ResponseEntity.ok(ApiResponse.success("Seller request rejected.", response));
    }

    @PostMapping("/{id}/resend-activation-otp")
    @Operation(summary = "Resend the guest seller activation OTP")
    public ResponseEntity<ApiResponse<SellerRequestApprovalResponse>> resendActivationOtp(@PathVariable Long id) {
        SellerRequestApprovalResponse response = approvalService.resendActivationOtp(id);
        return ResponseEntity.ok(ApiResponse.success("Activation OTP resent successfully.", response));
    }

    private Long resolveAdminUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }
        throw new IllegalStateException("Admin authentication context is missing.");
    }
}