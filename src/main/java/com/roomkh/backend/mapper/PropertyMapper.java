package com.roomkh.backend.mapper;

import com.roomkh.backend.dto.property.SellerPropertyListItemResponse;
import com.roomkh.backend.dto.property.SellerPropertyResponse;
import com.roomkh.backend.entity.Amenity;
import com.roomkh.backend.entity.Property;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PropertyMapper {

    public SellerPropertyResponse toSellerPropertyResponse(Property property) {
        List<String> amenityCodes = property.getAmenities().stream()
                .map(Amenity::getCode)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        return SellerPropertyResponse.builder()
                .id(property.getId())
                .title(property.getTitle())
                .slug(property.getSlug())
                .propertyType(property.getPropertyType())
                .purpose(property.getPurpose())
                .price(property.getPrice())
                .currency(property.getCurrency())
                .priceUnit(property.getPriceUnit())
                .status(property.getStatus())
                .description(property.getDescription())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .sizeSqm(property.getSizeSqm())
                .floor(property.getFloor())
                .furnished(property.isFurnished())
                .ageYears(property.getAgeYears())
                .address(property.getAddress())
                .province(property.getProvince())
                .district(property.getDistrict())
                .commune(property.getCommune())
                .latitude(property.getLatitude())
                .longitude(property.getLongitude())
                .amenities(amenityCodes)
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }

    public SellerPropertyListItemResponse toSellerPropertyListItemResponse(
            Property property,
            String coverImageUrl
    ) {
        return SellerPropertyListItemResponse.builder()
                .id(property.getId())
                .title(property.getTitle())
                .slug(property.getSlug())
                .propertyType(property.getPropertyType())
                .purpose(property.getPurpose())
                .price(property.getPrice())
                .currency(property.getCurrency())
                .priceUnit(property.getPriceUnit())
                .status(property.getStatus())
                .province(property.getProvince())
                .district(property.getDistrict())
                .commune(property.getCommune())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .sizeSqm(property.getSizeSqm())
                .coverImageUrl(coverImageUrl)
                .viewCount(property.getViewCount())
                .inquiryCount(property.getInquiryCount())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }
}