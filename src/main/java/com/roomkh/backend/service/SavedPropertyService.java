package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.ToggleSaveResponse;

public interface SavedPropertyService {
    ToggleSaveResponse toggleSaveProperty(Long userId, Long propertyId);
}