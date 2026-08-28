package com.roomkh.backend.controller;

import com.roomkh.backend.dto.comon.ApiResponse;
import com.roomkh.backend.dto.comon.PageMeta;
import com.roomkh.backend.dto.property.PublicPropertyDetailResponse;
import com.roomkh.backend.dto.property.PublicPropertyListItemResponse;
import com.roomkh.backend.service.PublicPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/properties")
@RequiredArgsConstructor
@Tag(name = "Public Properties", description = "Publicly accessible property search and listing endpoints")
public class PublicPropertyController {

    private final PublicPropertyService publicPropertyService;

    @GetMapping("/{propertyId}")
    @Operation(
            summary = "Get public property details",
            description = "Fetches the full details of an ACTIVE property for public display. Automatically increments the property's view count."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Property details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Property not found or unavailable")
    })
    public ResponseEntity<ApiResponse<PublicPropertyDetailResponse>> getPropertyDetail(@Parameter(description = "Property ID") @PathVariable Long propertyId) {
        PublicPropertyDetailResponse response = publicPropertyService.getPropertyDetail(propertyId);
        return ResponseEntity.ok(ApiResponse.success("Property details retrieved successfully.", response));
    }

    @GetMapping
    @Operation(
            summary = "Search public properties",
            description = "Retrieves a paginated list of ACTIVE properties. Supports filtering by type, purpose, price, and location."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Properties retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<PublicPropertyListItemResponse>>> searchProperties(
            @Parameter(description = "Page number, starting at 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page, 1-50") @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Filter by purpose (e.g., RENT, SALE)") @RequestParam(required = false) String purpose,
            @Parameter(description = "Filter by property type (e.g., ROOM, APARTMENT)") @RequestParam(name = "property_type", required = false) String propertyType,
            @Parameter(description = "Minimum price") @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by province") @RequestParam(required = false) String province,
            @Parameter(description = "Sort order: newest, price_asc, price_desc") @RequestParam(name = "sort_by", defaultValue = "newest") String sortBy) {

        Page<PublicPropertyListItemResponse> resultPage = publicPropertyService.searchProperties(
                page, size, purpose, propertyType, minPrice, maxPrice, province, sortBy);
                
        PageMeta meta = PageMeta.from(resultPage);

        return ResponseEntity.ok(ApiResponse.success(
                "Properties retrieved successfully.", 
                resultPage.getContent(), 
                meta));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{propertyId}/contact-clicks")
    @Operation(
            summary = "Record property contact click",
            description = "Records a user click on contact buttons (e.g., Telegram, Call, WhatsApp) on the property detail page, incrementing the property's inquiry_count for seller analytics."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contact click recorded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Property not found or unavailable")
    })
    public ResponseEntity<ApiResponse<Void>> recordContactClick(
            @io.swagger.v3.oas.annotations.Parameter(description = "Property ID")
            @org.springframework.web.bind.annotation.PathVariable Long propertyId) {

        publicPropertyService.recordContactClick(propertyId);
        return ResponseEntity.ok(ApiResponse.success("Contact click recorded successfully.", null));
    }
}