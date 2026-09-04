package com.roomkh.backend.controller;

import com.roomkh.backend.dto.analytics.PlatformGrowthDto;
// import com.roomkh.backend.dto.common.ApiResponse; // បើកវិញបើប្រូមាន ApiResponse ប្រើទូទៅ
import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.service.DashboardStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final DashboardStatService dashboardStatService;

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get analytics data",
            description = "Retrieves the data for platform growth information",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getPlatformGrowth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<PlatformGrowthDto> growthData = dashboardStatService.getPlatformGrowthData(startDate, endDate);

        return ResponseEntity.ok(new ApiResponse<>(true, "Success", growthData));
    }
}