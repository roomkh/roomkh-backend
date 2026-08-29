package com.roomkh.backend.controller;

import com.roomkh.backend.dto.admin.AdminCreateUserRequest;
import com.roomkh.backend.dto.admin.AdminDashboardStatsResponse;
import com.roomkh.backend.dto.admin.AdminUserListItemResponse;
import com.roomkh.backend.dto.admin.UpdateUserStatusRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Endpoints for admin analytics and management")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

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