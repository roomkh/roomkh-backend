package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.HomeDataResponse;
import com.roomkh.backend.dto.property.PublicPropertyDetailResponse;
import com.roomkh.backend.dto.property.PublicPropertyListItemResponse;
import com.roomkh.backend.dto.property.PublicSellerProfileResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface PublicPropertyService {
    Page<PublicPropertyListItemResponse> searchProperties(
            int page, int size, String purpose, String propertyType, 
            BigDecimal minPrice, BigDecimal maxPrice, String location, String sortBy,
            Integer minBedrooms, Integer maxBedrooms, Integer minBathrooms, Integer maxBathrooms);

    PublicPropertyDetailResponse getPropertyDetail(Long propertyId);

    void recordContactClick(Long propertyId);

    List<PublicPropertyListItemResponse> getSimilarProperties(Long propertyId);

    List<PublicPropertyListItemResponse> getOwnerProperties(Long propertyId);

    /** Public profile of an active seller with all their ACTIVE listings; 404 for anyone else. */
    PublicSellerProfileResponse getSellerProfile(Long sellerId);

    HomeDataResponse getHomeData();

    List<String> getAvailableLocations();
}