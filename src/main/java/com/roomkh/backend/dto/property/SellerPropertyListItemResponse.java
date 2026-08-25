package com.roomkh.backend.dto.property;

import com.roomkh.backend.entity.PriceUnit;
import com.roomkh.backend.entity.PropertyPurpose;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPropertyListItemResponse {
    private Long id;
    private String title;
    private String slug;
    private PropertyType propertyType;
    private PropertyPurpose purpose;
    private BigDecimal price;
    private String currency;
    private PriceUnit priceUnit;
    private PropertyStatus status;
    private String province;
    private String district;
    private String commune;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal sizeSqm;
    private String coverImageUrl;
    private Long viewCount;
    private Long inquiryCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}