package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.comon.PageMeta;
import com.roomkh.backend.dto.seller.SellerRequestResponse;
import com.roomkh.backend.dto.seller.SellerRequestSummaryResponse;
import com.roomkh.backend.entity.SellerRequestStatus;
import com.roomkh.backend.service.SellerRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/seller-requests")
@RequiredArgsConstructor
@Tag(name = "Admin - Seller Requests", description = "Admin review of seller requests")
public class AdminSellerRequestController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SellerRequestService sellerRequestService;

    @GetMapping
    @Operation(summary = "List seller requests with optional filters")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<SellerRequestSummaryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) SellerRequestStatus status,
            @RequestParam(required = false) String keyword) {

        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize);

        Page<SellerRequestSummaryResponse> result = sellerRequestService.list(status, keyword, pageable);
        PageMeta meta = PageMeta.from(result);

        return ResponseEntity.ok(ApiResponse.success("Seller requests retrieved successfully.", result.getContent(), meta));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get seller request detail")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<SellerRequestResponse>> getById(@PathVariable Long id) {
        SellerRequestResponse response = sellerRequestService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Seller request retrieved successfully.", response));
    }
}