package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.property.SellerDashboardResponse;
import com.roomkh.backend.security.CustomUserDetails;
import com.roomkh.backend.service.SellerPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/dashboard")
@RequiredArgsConstructor
@Tag(name = "Seller Dashboard", description = "Endpoints for seller dashboard statistics")
public class SellerDashboardController {

    private final SellerPropertyService sellerPropertyService;

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get seller dashboard summary",
            description = "Fetches summary statistics for the authenticated SELLER's dashboard, including property counts by status, total views, and total inquiries."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller dashboard retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not a SELLER")
    })
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getDashboardSummary() {
        Long authenticatedUserId = resolveAuthenticatedUserId();
        SellerDashboardResponse response = sellerPropertyService.getDashboardSummary(authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.success("Seller dashboard retrieved successfully.", response));
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }
        throw new IllegalStateException("Authentication context is missing.");
    }
}