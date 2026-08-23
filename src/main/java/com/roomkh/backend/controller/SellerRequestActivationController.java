package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.seller.ActivateGuestSellerRequest;
import com.roomkh.backend.dto.seller.SellerActivationResponse;
import com.roomkh.backend.service.SellerRequestApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller-requests")
@RequiredArgsConstructor
@Tag(name = "Seller Request Activation", description = "Guest seller account activation via OTP")
public class SellerRequestActivationController {

    private final SellerRequestApprovalService approvalService;

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a guest seller account using OTP")
    public ResponseEntity<ApiResponse<SellerActivationResponse>> activate(
            @PathVariable Long id,
            @Valid @RequestBody ActivateGuestSellerRequest request) {

        SellerActivationResponse response = approvalService.activate(
                id, request.getOtpCode(), request.getPassword(), request.getConfirmPassword());

        return ResponseEntity.ok(ApiResponse.success("Seller account activated successfully. You can now log in.", response));
    }
}