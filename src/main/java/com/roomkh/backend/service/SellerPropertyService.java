package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.CreatePropertyRequest;
import com.roomkh.backend.dto.property.SellerPropertyResponse;

public interface SellerPropertyService {
    SellerPropertyResponse createDraft(Long authenticatedUserId, CreatePropertyRequest request);
}