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
public class ToggleSaveResponse {

    @JsonProperty("property_id")
    private Long propertyId;

    @JsonProperty("is_saved")
    private boolean isSaved;
}