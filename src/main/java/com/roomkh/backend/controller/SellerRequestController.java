package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.seller.CreateSellerRequest;
import com.roomkh.backend.dto.seller.SellerRequestResponse;
import com.roomkh.backend.security.CustomUserDetails;
import com.roomkh.backend.service.SellerRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller-requests")
@RequiredArgsConstructor
@Tag(name = "Seller Requests", description = "Submit a request to become a seller")
public class SellerRequestController {

    private final SellerRequestService sellerRequestService;

    @PostMapping
    @Operation(summary = "Submit a seller request (guest or logged-in USER)")
    public ResponseEntity<ApiResponse<SellerRequestResponse>> submit(@Valid @RequestBody CreateSellerRequest request) {
        Long authenticatedUserId = resolveAuthenticatedUserId();
        SellerRequestResponse response = sellerRequestService.submit(request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seller request submitted successfully.", response));
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }
        return null;
    }
}