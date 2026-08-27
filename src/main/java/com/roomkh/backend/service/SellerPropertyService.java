package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.*;
import com.roomkh.backend.entity.PropertyStatus;

public interface SellerPropertyService {
    SellerPropertyResponse createDraft(Long authenticatedUserId, CreatePropertyRequest request);

    SellerPropertyListResult listProperties(Long authenticatedUserId, PropertyStatus status,
                                            int page, int size, String sortBy);

    SellerPropertyResponse updateProperty(Long authenticatedUserId, Long propertyId, UpdatePropertyRequest request);

    SellerPropertyResponse submitPropertyForReview(Long authenticatedUserId, Long propertyId);

    SellerPropertyResponse updatePropertyStatus(Long authenticatedUserId, Long propertyId, UpdatePropertyStatusRequest request);

    void deleteProperty(Long authenticatedUserId, Long propertyId);

    SellerPropertyDetailResponse getPropertyDetail(Long authenticatedUserId, Long propertyId);

    SellerDashboardResponse getDashboardSummary(Long authenticatedUserId);
}