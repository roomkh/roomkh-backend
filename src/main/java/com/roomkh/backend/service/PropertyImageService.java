package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.PropertyImageDeleteResponse;
import com.roomkh.backend.dto.property.PropertyImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PropertyImageService {

    PropertyImageUploadResponse uploadImage(
            Long authenticatedUserId,
            Long propertyId,
            MultipartFile image,
            Boolean isCover,
            Integer sortOrder
    );

    PropertyImageDeleteResponse deleteImage(
            Long authenticatedUserId,
            Long propertyId,
            Long imageId
    );
}