package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.comon.PageMeta;
import com.roomkh.backend.dto.property.AdminPropertyListItemResponse;
import com.roomkh.backend.dto.property.AdminPropertyReviewRequest;
import com.roomkh.backend.security.CustomUserDetails;
import com.roomkh.backend.service.AdminPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/properties")
@RequiredArgsConstructor
@Tag(name = "Admin Properties", description = "Endpoints for platform administrators to manage properties")
public class AdminPropertyController {

    private final AdminPropertyService adminPropertyService;

    @PatchMapping("/{propertyId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Approve or reject a property",
            description = "Allows an ADMIN to approve (set to ACTIVE) or reject (set to REJECTED) a property currently in PENDING status. Records audit information."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Property reviewed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition (not PENDING) or missing rejection reason"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<ApiResponse<Void>> reviewProperty(
            @PathVariable Long propertyId,
            @Valid @RequestBody AdminPropertyReviewRequest request) {

        Long adminId = resolveAuthenticatedUserId();
        adminPropertyService.reviewProperty(adminId, propertyId, request);
        return ResponseEntity.ok(ApiResponse.success("Property reviewed successfully.", null));
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }
        throw new IllegalStateException("Authentication context is missing.");
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get list of all properties",
            description = "Retrieves a paginated list of properties across the platform for admin review. Supports filtering by status (e.g., PENDING) and sorting."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin properties retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN")
    })
    public ResponseEntity<ApiResponse<List<AdminPropertyListItemResponse>>> listProperties(
            @Parameter(description = "Filter by status: PENDING, ACTIVE, REJECTED, etc.")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number, starting at 1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page, 1-50")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort order: newest, recently_submitted")
            @RequestParam(name = "sort_by", defaultValue = "newest") String sortBy) {

        Page<AdminPropertyListItemResponse> resultPage = adminPropertyService.listProperties(status, page, size, sortBy);
        PageMeta meta = PageMeta.from(resultPage);

        return ResponseEntity.ok(ApiResponse.success(
                "Admin properties retrieved successfully.", 
                resultPage.getContent(), 
                meta));
    }
}