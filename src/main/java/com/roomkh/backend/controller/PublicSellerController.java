package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.property.PublicSellerProfileResponse;
import com.roomkh.backend.service.PublicPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/sellers")
@RequiredArgsConstructor
@Tag(name = "Public Sellers", description = "Public seller profiles")
public class PublicSellerController {

    private final PublicPropertyService publicPropertyService;

    @GetMapping("/{sellerId}")
    @Operation(
            summary = "Get a seller's public profile",
            description = "Returns an active seller's name, avatar, phone, join date and all of their ACTIVE listings (newest first)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No active seller with this ID")
    })
    public ResponseEntity<ApiResponse<PublicSellerProfileResponse>> getSellerProfile(
            @Parameter(description = "Seller (user) ID") @PathVariable Long sellerId) {
        PublicSellerProfileResponse response = publicPropertyService.getSellerProfile(sellerId);
        return ResponseEntity.ok(ApiResponse.success("Seller profile retrieved successfully.", response));
    }
}
