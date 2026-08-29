package com.roomkh.backend.controller;

import com.roomkh.backend.dto.admin.AdminDashboardStatsResponse;
import com.roomkh.backend.dto.admin.AdminUserListItemResponse;
import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Endpoints for admin analytics and management")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

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
}