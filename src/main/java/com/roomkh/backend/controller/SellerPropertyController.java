package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.property.CreatePropertyRequest;
import com.roomkh.backend.dto.property.SellerPropertyResponse;
import com.roomkh.backend.security.CustomUserDetails;
import com.roomkh.backend.service.SellerPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/v1/seller/properties")
@RequiredArgsConstructor
@Tag(name = "Seller Properties", description = "SELLER-only property management")
public class SellerPropertyController {

    private final SellerPropertyService sellerPropertyService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create a new property draft",
            description = "Creates a property owned by the authenticated SELLER. The property always starts in DRAFT " +
                    "status regardless of any status value sent in the request. Requires a valid JWT with the SELLER role; " +
                    "USER and ADMIN tokens receive 403 Forbidden."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Property draft created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or invalid amenity code"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not a SELLER")
    })
    public ResponseEntity<ApiResponse<SellerPropertyResponse>> createDraft(@Valid @RequestBody CreatePropertyRequest request) {
        Long authenticatedUserId = resolveAuthenticatedUserId();
        SellerPropertyResponse response = sellerPropertyService.createDraft(authenticatedUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Property draft created successfully.", response));
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }
        throw new IllegalStateException("Authentication context is missing.");
    }
}