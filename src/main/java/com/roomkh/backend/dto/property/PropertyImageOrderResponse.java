package com.roomkh.backend.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyImageOrderResponse {
    private Long id;
    private Long propertyId;
    private String url;
    private boolean isCover;
    private Integer sortOrder;
}