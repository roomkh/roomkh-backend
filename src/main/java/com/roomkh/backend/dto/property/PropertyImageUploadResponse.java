package com.roomkh.backend.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyImageUploadResponse {
    private Long id;
    private Long propertyId;
    private String url;
    private boolean cover;
    private Integer sortOrder;
    private String contentType;
    private Long fileSize;
    private OffsetDateTime createdAt;
}