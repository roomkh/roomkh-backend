package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.PublicPropertyListItemResponse;
import com.roomkh.backend.dto.property.ToggleSaveResponse;
import org.springframework.data.domain.Page;

public interface SavedPropertyService {
    ToggleSaveResponse toggleSaveProperty(Long userId, Long propertyId);

    Page<PublicPropertyListItemResponse> getSavedProperties(Long userId, int page, int size);
}