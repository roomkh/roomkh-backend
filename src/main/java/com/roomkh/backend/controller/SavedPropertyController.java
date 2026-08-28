package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.property.PublicPropertyListItemResponse;
import com.roomkh.backend.dto.property.ToggleSaveResponse;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.SavedPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/user/saved-properties")
@RequiredArgsConstructor
@Tag(name = "Saved Properties", description = "Endpoints for users to manage their bookmarked/saved properties")
public class SavedPropertyController {

    private final SavedPropertyService savedPropertyService;
    private final UserRepository userRepository;

    @PostMapping("/{propertyId}/toggle")
    @Operation(
            summary = "Toggle save property",
            description = "Saves the property if it is not saved, or unsaves it if it is already saved.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Property save status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Property not found or unavailable")
    })
    public ResponseEntity<ApiResponse<ToggleSaveResponse>> toggleSaveProperty(
            @Parameter(description = "Property ID") @PathVariable Long propertyId,
            Principal principal) {

//        System.out.println("====== PRINCIPAL NAME IS: " + principal.getName() + " ======");

        Long currentUserId = Long.valueOf(principal.getName());
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        ToggleSaveResponse response = savedPropertyService.toggleSaveProperty(user.getId(), propertyId);
        return ResponseEntity.ok(ApiResponse.success("Property save status updated successfully.", response));
    }

    @GetMapping
    @Operation(
            summary = "Get saved properties",
            description = "Fetches a paginated list of properties saved (bookmarked) by the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Saved properties retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required")
    })
    public ResponseEntity<com.roomkh.backend.dto.comon.ApiResponse<Page<PublicPropertyListItemResponse>>> getSavedProperties(
            @Parameter(description = "Page number, starting at 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int size,
            Principal principal) {

        Long currentUserId = Long.valueOf(principal.getName());
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        Page<PublicPropertyListItemResponse> response = savedPropertyService.getSavedProperties(user.getId(), page, size);
        return ResponseEntity.ok(com.roomkh.backend.dto.comon.ApiResponse.success("Saved properties retrieved successfully.", response));
    }
}