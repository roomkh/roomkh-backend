package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.PropertyImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PropertyImageService {
    PropertyImageUploadResponse uploadImage(Long authenticatedUserId, Long propertyId, MultipartFile image,
                                             Boolean isCover, Integer sortOrder);
}