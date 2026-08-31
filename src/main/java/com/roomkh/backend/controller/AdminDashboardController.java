package com.roomkh.backend.controller;

import com.roomkh.backend.dto.admin.*;
import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Endpoints for admin analytics and management")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/properties/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Export properties to Excel",
            description = "Downloads a comprehensive Excel report of all properties.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<byte[]> exportPropertiesToExcel() {
        byte[] excelData = adminDashboardService.exportPropertiesToExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"properties_export.xlsx\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @DeleteMapping("/properties/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Soft delete (Ban) property",
            description = "Soft deletes a property by changing its status to BANNED.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> softDeleteProperty(
            @Parameter(description = "Property ID")
            @PathVariable Long id) {

        adminDashboardService.softDeleteProperty(id);
        return ResponseEntity.ok(ApiResponse.success("Property has been banned/deleted successfully.", null));
    }

    @GetMapping("/properties/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get property details (Admin)",
            description = "Retrieves complete nested details of a specific property for moderation purposes.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AdminPropertyDetailResponse>> getPropertyDetail(
            @Parameter(description = "Property ID")
            @PathVariable Long id) {

        AdminPropertyDetailResponse response = adminDashboardService.getPropertyDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Property details retrieved successfully.", response));
    }

    @GetMapping("/properties/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get property statistics",
            description = "Calculates total, active, pending, and inactive property counts and trends based on a specific date range.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AdminPropertyStatsResponse>> getPropertyStats(
            @Parameter(description = "Start date in format YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date in format YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AdminPropertyStatsResponse response = adminDashboardService.getPropertyStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Property statistics retrieved successfully.", response));
    }

    @GetMapping("/properties")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all properties (Admin)",
            description = "Retrieves a paginated list of properties with UI filters for search, status, type, and city.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<AdminDashboardPropertyResponse>>> getProperties(
            @Parameter(description = "Search by title, location, or owner name")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by status (e.g., ACTIVE, PENDING)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by property type")
            @RequestParam(required = false) String type,

            @Parameter(description = "Filter by city")
            @RequestParam(required = false) String city,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Page number, starting at 1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Items per page")
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminDashboardPropertyResponse> response = adminDashboardService.getProperties(search, status, type, city, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success("Properties retrieved successfully.", response));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Manually create a new user or seller",
            description = "Allows administrators to bypass public registration and instantly create activated user or approved seller accounts.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        adminDashboardService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User created successfully.", null));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all users (Admin)",
            description = "Retrieves a paginated list of users with optional filtering by search term and role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    public ResponseEntity<ApiResponse<Page<AdminUserListItemResponse>>> getUsers(
            @Parameter(description = "Search by name, email, or phone")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by exact role (e.g., USER, SELLER, ADMIN)")
            @RequestParam(required = false) RoleName role,

            @Parameter(description = "Filter by status (e.g., ACTIVE, INACTIVE, PENDING)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Page number, starting at 1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Items per page")
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminUserListItemResponse> response = adminDashboardService.getUsers(search, role, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully.", response));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get admin dashboard statistics",
            description = "Retrieves aggregated counts for users, owners, listings, and revenue for the admin overview.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard stats retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        AdminDashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully.", stats));
    }

    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update user or seller status",
            description = "Allows an admin to update a user's account status (ACTIVATE, INACTIVE, BAN) or approve/reject a pending seller.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        adminDashboardService.updateUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully.", null));
    }
}