package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.HomeDataResponse;
import com.roomkh.backend.dto.property.PublicPropertyDetailResponse;
import com.roomkh.backend.dto.property.PublicPropertyListItemResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface PublicPropertyService {
    Page<PublicPropertyListItemResponse> searchProperties(
            int page, int size, String purpose, String propertyType, 
            BigDecimal minPrice, BigDecimal maxPrice, String location, String sortBy);

    PublicPropertyDetailResponse getPropertyDetail(Long propertyId);

    void recordContactClick(Long propertyId);

    List<PublicPropertyListItemResponse> getSimilarProperties(Long propertyId);

    HomeDataResponse getHomeData();

    List<String> getAvailableLocations();
}