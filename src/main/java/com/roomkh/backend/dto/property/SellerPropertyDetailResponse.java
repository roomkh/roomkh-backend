package com.roomkh.backend.dto.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.roomkh.backend.entity.PriceUnit;
import com.roomkh.backend.entity.PropertyPurpose;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPropertyDetailResponse {

    private Long id;
    private String title;
    private PropertyStatus status;
    private PropertyPurpose purpose;
    
    @JsonProperty("property_type")
    private PropertyType propertyType;
    
    private BigDecimal price;
    private String currency;
    
    @JsonProperty("price_unit")
    private PriceUnit priceUnit;
    
    private String description;
    
    private Integer bedrooms;
    private Integer bathrooms;
    
    @JsonProperty("size_sqm")
    private BigDecimal sizeSqm;
    
    private Integer floor;
    private boolean furnished;
    
    @JsonProperty("age_years")
    private Integer ageYears;
    
    private String address;
    private String province;
    private String district;
    private String commune;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    @JsonProperty("view_count")
    private Long viewCount;
    
    @JsonProperty("inquiry_count")
    private Long inquiryCount;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    
    @JsonProperty("submitted_at")
    private OffsetDateTime submittedAt;
    
    @JsonProperty("listed_at")
    private OffsetDateTime listedAt;
    
    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("amenity_codes")
    private List<String> amenityCodes;

    private List<PropertyImageResponse> images;
}