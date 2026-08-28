package com.roomkh.backend.dto.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSummaryResponse {

    private String province;
    
    @JsonProperty("property_count")
    private Long propertyCount;
}