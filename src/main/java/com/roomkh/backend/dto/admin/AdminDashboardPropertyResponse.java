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
public class AdminDashboardPropertyResponse {

    private Long id;

    @JsonProperty("property_code")
    private String propertyCode;

    private String title;

    @JsonProperty("owner_name")
    private String ownerName;

    @JsonProperty("owner_id")
    private String ownerId;

    @JsonProperty("owner_avatar_url")
    private String ownerAvatarUrl;

    private String type;

    private String location;

    private java.math.BigDecimal price;

    private String status;

    @JsonProperty("cover_image_url")
    private String coverImageUrl;
}