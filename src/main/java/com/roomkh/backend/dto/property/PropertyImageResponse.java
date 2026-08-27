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
public class PropertyImageResponse {

    private Long id;
    
    private String url;
    
    @JsonProperty("is_cover")
    private boolean isCover;
    
    @JsonProperty("sort_order")
    private int sortOrder;
}