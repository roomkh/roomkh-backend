package com.roomkh.backend.dto.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeDataResponse {

    @JsonProperty("featured_properties")
    private List<PublicPropertyListItemResponse> featuredProperties;

    private List<LocationSummaryResponse> locations;
}