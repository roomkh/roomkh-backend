package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.PropertyImageDeleteResponse;
import com.roomkh.backend.dto.property.PropertyImageOrderResponse;
import com.roomkh.backend.dto.property.PropertyImageUploadResponse;
import com.roomkh.backend.dto.property.ReorderPropertyImagesRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    List<PropertyImageOrderResponse> reorderImages(
            Long authenticatedUserId,
            Long propertyId,
            ReorderPropertyImagesRequest request
    );
}