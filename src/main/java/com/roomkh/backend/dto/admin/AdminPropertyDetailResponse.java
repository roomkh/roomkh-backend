package com.roomkh.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPropertyDetailResponse {

    private Long id;

    @JsonProperty("property_code")
    private String propertyCode;

    private String title;

    private String description;

    private String status;

    private String type;

    private String purpose;

    private java.math.BigDecimal price;

    private String currency;

    @JsonProperty("price_unit")
    private String priceUnit;

    private String location;

    private String address;

    private Integer bedrooms;

    private Integer bathrooms;

    @JsonProperty("size_sqm")
    private java.math.BigDecimal sizeSqm;

    private Integer floor;

    private boolean furnished;

    @JsonProperty("owner_id")
    private String ownerId;

    @JsonProperty("owner_name")
    private String ownerName;

    @JsonProperty("owner_phone")
    private String ownerPhone;

    @JsonProperty("owner_email")
    private String ownerEmail;

    @JsonProperty("owner_avatar_url")
    private String ownerAvatarUrl;

    @JsonProperty("cover_image_url")
    private String coverImageUrl;

    @JsonProperty("image_urls")
    private java.util.List<String> imageUrls;

    private java.util.List<String> amenities;

    @JsonProperty("created_at")
    private java.time.OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private java.time.OffsetDateTime updatedAt;
}