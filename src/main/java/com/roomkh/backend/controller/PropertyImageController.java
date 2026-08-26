package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.property.PropertyImageDeleteResponse;
import com.roomkh.backend.dto.property.PropertyImageUploadResponse;
import com.roomkh.backend.security.CustomUserDetails;
import com.roomkh.backend.service.PropertyImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/seller/properties/{propertyId}/images")
@RequiredArgsConstructor
@Tag(name = "Seller Property Images", description = "SELLER-only property image management")
public class PropertyImageController {

    private final PropertyImageService propertyImageService;

    @PostMapping(consumes = "multipart/form-data")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Upload a property image",
            description = "Uploads a JPG or PNG image (max 5 MB) for the authenticated SELLER's own DRAFT or " +
                    "REJECTED property. The first uploaded image automatically becomes the cover image."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Image uploaded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid, oversized, or unsupported image file"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not a SELLER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Property not found or not owned by seller"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Non-editable property status or duplicate sort order"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Image storage is not configured (production)")
    })
    public ResponseEntity<ApiResponse<PropertyImageUploadResponse>> uploadImage(
            @PathVariable Long propertyId,
            @Parameter(description = "Image file (JPG or PNG, max 5 MB)", required = true)
            @RequestParam("image") MultipartFile image,
            @Parameter(description = "Whether this image should become the cover image")
            @RequestParam(name = "is_cover", required = false) Boolean isCover,
            @Parameter(description = "Explicit sort order (optional; auto-assigned if omitted)")
            @RequestParam(name = "sort_order", required = false) Integer sortOrder) {

        Long authenticatedUserId = resolveAuthenticatedUserId();

        PropertyImageUploadResponse response = propertyImageService.uploadImage(
                authenticatedUserId,
                propertyId,
                image,
                isCover,
                sortOrder
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Property image uploaded successfully.", response));
    }

    @DeleteMapping("/{imageId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Delete a property image",
            description = "Deletes an image from the authenticated SELLER's own DRAFT or REJECTED property. " +
                    "If the deleted image is the current cover, the remaining image with the lowest sort order " +
                    "is promoted to cover automatically."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not a SELLER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Property or property image not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Property status is not editable")
    })
    public ResponseEntity<ApiResponse<PropertyImageDeleteResponse>> deleteImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {

        Long authenticatedUserId = resolveAuthenticatedUserId();

        PropertyImageDeleteResponse response = propertyImageService.deleteImage(
                authenticatedUserId,
                propertyId,
                imageId
        );

        return ResponseEntity.ok(
                ApiResponse.success("Property image deleted successfully.", response)
        );
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }

        throw new IllegalStateException("Authentication context is missing.");
    }
}