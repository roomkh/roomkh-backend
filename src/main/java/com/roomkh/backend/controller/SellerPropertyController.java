package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.comon.PageMeta;
import com.roomkh.backend.dto.property.CreatePropertyRequest;
import com.roomkh.backend.dto.property.SellerPropertyListItemResponse;
import com.roomkh.backend.dto.property.SellerPropertyResponse;
import com.roomkh.backend.dto.property.UpdatePropertyRequest;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.security.CustomUserDetails;
import com.roomkh.backend.service.SellerPropertyListResult;
import com.roomkh.backend.service.SellerPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/properties")
@RequiredArgsConstructor
@Tag(name = "Seller Properties", description = "SELLER-only property management")
public class SellerPropertyController {

    private final SellerPropertyService sellerPropertyService;


    @PostMapping("/{propertyId}/submit")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Submit property for review",
            description = "Submits a DRAFT or REJECTED property for Admin review. Updates status to PENDING. " +
                    "Property must have at least one image and one cover image."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Property submitted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing images, cover image, or invalid status transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not a SELLER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Property not found or not owned by seller")
    })
    public ResponseEntity<ApiResponse<SellerPropertyResponse>> submitProperty(@PathVariable Long propertyId) {
        Long authenticatedUserId = resolveAuthenticatedUserId();
        SellerPropertyResponse response = sellerPropertyService.submitPropertyForReview(authenticatedUserId, propertyId);
        return ResponseEntity.ok(ApiResponse.success("Property submitted for review successfully.", response));
    }

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

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "List the authenticated seller's own properties",
            description = "Returns a paginated, sortable list of properties owned by the authenticated SELLER, " +
                    "along with status counts for the Seller Home tabs. USER and ADMIN tokens receive 403 Forbidden."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated seller property list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status, page, size, or sort_by value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not a SELLER")
    })
    public ResponseEntity<ApiResponse<List<SellerPropertyListItemResponse>>> list(
            @Parameter(description = "Filter by status: DRAFT, PENDING, ACTIVE, REJECTED, SOLD_RENTED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number, starting at 1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page, 1-50")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort order: recently_updated, newest, price_asc, price_desc")
            @RequestParam(name = "sort_by", defaultValue = "recently_updated") String sortBy) {

        PropertyStatus parsedStatus = parseStatus(status);
        Long authenticatedUserId = resolveAuthenticatedUserId();

        SellerPropertyListResult result = sellerPropertyService.listProperties(
                authenticatedUserId, parsedStatus, page, size, sortBy);

        PageMeta meta = PageMeta.from(result.getPage(), result.getStatusCounts());

        return ResponseEntity.ok(ApiResponse.success(
                "Seller properties retrieved successfully.", result.getPage().getContent(), meta));
    }

    @PutMapping("/{propertyId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update an existing property",
            description = "Updates a property owned by the authenticated SELLER. Only DRAFT or REJECTED properties " +
                    "can be updated; PENDING, ACTIVE, and SOLD_RENTED properties return 409. The property's status " +
                    "itself is never changed by this endpoint."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Property updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or invalid amenity code"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not a SELLER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Property not found or not owned by seller"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Property status is not editable")
    })
    public ResponseEntity<ApiResponse<SellerPropertyResponse>> updateProperty(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdatePropertyRequest request) {

        Long authenticatedUserId = resolveAuthenticatedUserId();
        SellerPropertyResponse response = sellerPropertyService.updateProperty(authenticatedUserId, propertyId, request);
        return ResponseEntity.ok(ApiResponse.success("Property updated successfully.", response));
    }

    private PropertyStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return PropertyStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value. Allowed values: DRAFT, PENDING, ACTIVE, REJECTED, SOLD_RENTED.");
        }
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }
        throw new IllegalStateException("Authentication context is missing.");
    }
}