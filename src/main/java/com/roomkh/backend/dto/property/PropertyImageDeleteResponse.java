package com.roomkh.backend.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyImageDeleteResponse {
    private Long id;
    private Long propertyId;
    private boolean wasCover;
    private Long newCoverImageId;
}