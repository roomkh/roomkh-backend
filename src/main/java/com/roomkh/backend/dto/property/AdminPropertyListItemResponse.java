package com.roomkh.backend.dto.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.roomkh.backend.entity.PriceUnit;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPropertyListItemResponse {

    private Long id;
    private String title;
    private PropertyStatus status;
    
    @JsonProperty("property_type")
    private PropertyType propertyType;
    
    private BigDecimal price;
    private String currency;
    
    @JsonProperty("price_unit")
    private PriceUnit priceUnit;

    @JsonProperty("submitted_at")
    private OffsetDateTime submittedAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("cover_image_url")
    private String coverImageUrl;

    @JsonProperty("owner_id")
    private Long ownerId;

    @JsonProperty("owner_name")
    private String ownerName;

    @JsonProperty("owner_email")
    private String ownerEmail;

    @JsonProperty("owner_phone")
    private String ownerPhone;
}