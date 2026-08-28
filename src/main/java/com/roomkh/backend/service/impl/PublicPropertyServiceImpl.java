package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.*;
import com.roomkh.backend.entity.*;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.PropertyImageRepository;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.PropertySpecification;
import com.roomkh.backend.service.PublicPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicPropertyServiceImpl implements PublicPropertyService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;


    @Override
    @Transactional(readOnly = true)
    public List<PublicPropertyListItemResponse> getSimilarProperties(Long propertyId) {
        // 1. Fetch reference property
        Property referenceProperty = propertyRepository.findByIdAndStatus(propertyId, PropertyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Reference property not found or unavailable."));

        // 2. Query similar properties with a limit of 4
        Pageable limit = PageRequest.of(0, 4);
        List<Property> similarProperties = propertyRepository.findSimilarProperties(
                referenceProperty.getId(),
                referenceProperty.getPropertyType(),
                referenceProperty.getProvince(),
                limit
        );

        if (similarProperties.isEmpty()) {
            return List.of();
        }

        // 3. Fetch cover images in bulk
        List<Long> similarPropertyIds = similarProperties.stream().map(Property::getId).toList();
        Map<Long, String> coverImageUrls = propertyImageRepository.findByProperty_IdInAndCoverTrue(similarPropertyIds).stream()
                .collect(Collectors.toMap(
                        img -> img.getProperty().getId(),
                        PropertyImage::getUrl
                ));

        // 4. Map to DTO
        return similarProperties.stream()
                .map(property -> PublicPropertyListItemResponse.builder()
                        .id(property.getId())
                        .title(property.getTitle())
                        .purpose(property.getPurpose())
                        .propertyType(property.getPropertyType())
                        .price(property.getPrice())
                        .currency(property.getCurrency())
                        .priceUnit(property.getPriceUnit())
                        .bedrooms(property.getBedrooms())
                        .bathrooms(property.getBathrooms())
                        .sizeSqm(property.getSizeSqm())
                        .province(property.getProvince())
                        .district(property.getDistrict())
                        .commune(property.getCommune())
                        .coverImageUrl(coverImageUrls.get(property.getId()))
                        .publishedAt(resolvePublishedAt(property)) // Utilizing the existing helper method
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public PublicPropertyDetailResponse getPropertyDetail(Long propertyId) {
        // 1. Fetch property and enforce ACTIVE visibility rule
        Property property = propertyRepository.findByIdAndStatus(propertyId, PropertyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found or unavailable."));

        // 2. Increment view count
        property.setViewCount((property.getViewCount() == null ? 0 : property.getViewCount()) + 1);

        // 3. Fetch related images and sort by sortOrder
        List<PropertyImage> images = propertyImageRepository.findByProperty_Id(propertyId);
        if (images != null) {
            images.sort(java.util.Comparator.comparingInt(PropertyImage::getSortOrder));
        }

        // 4. Map Images
        List<PropertyImageResponse> imageResponses = images == null ? List.of() : images.stream()
                .map(img -> PropertyImageResponse.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .isCover(img.isCover())
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();

        // 5. Map Amenities
        List<String> amenityCodes = property.getAmenities() == null ? List.of() : property.getAmenities().stream()
                .map(Amenity::getCode)
                .toList();

        // 6. Map Seller Contact
        User seller = property.getSeller();
        SellerContactResponse sellerContact = seller != null ? SellerContactResponse.builder()
                .id(seller.getId())
                .name(seller.getFullName())
                .phoneNumber(seller.getPhoneNumber())
                .email(seller.getEmail())
                .build() : null;

        // 7. Assemble DTO
        return PublicPropertyDetailResponse.builder()
                .id(property.getId())
                .title(property.getTitle())
                .purpose(property.getPurpose())
                .propertyType(property.getPropertyType())
                .price(property.getPrice())
                .currency(property.getCurrency())
                .priceUnit(property.getPriceUnit())
                .description(property.getDescription())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .sizeSqm(property.getSizeSqm())
                .floor(property.getFloor())
                .furnished(property.isFurnished())
                .ageYears(property.getAgeYears())
                .province(property.getProvince())
                .district(property.getDistrict())
                .commune(property.getCommune())
                .address(property.getAddress())
                .latitude(property.getLatitude())
                .longitude(property.getLongitude())
                .viewCount(property.getViewCount())
                .publishedAt(resolvePublishedAt(property))
                .amenityCodes(amenityCodes)
                .images(imageResponses)
                .seller(sellerContact)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PublicPropertyListItemResponse> searchProperties(
            int page, int size, String purpose, String propertyType, 
            BigDecimal minPrice, BigDecimal maxPrice, String location, String sortBy) {
            
        if (page < 1) {
            throw new BadRequestException("page must be at least 1.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and 50.");
        }

        // 1. Resolve Sorting
        Sort sort = resolveSort(sortBy);
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // 2. Build Specification (Status = ACTIVE is enforced inside)
        Specification<Property> spec = PropertySpecification.filterPublicProperties(
                purpose, propertyType, minPrice, maxPrice, location);

        // 3. Fetch Properties
        Page<Property> propertyPage = propertyRepository.findAll(spec, pageable);

        List<Long> propertyIds = propertyPage.getContent().stream()
                .map(Property::getId)
                .toList();

        // 4. Bulk Fetch Cover Images (Prevent N+1)
        Map<Long, String> coverImageUrls = propertyIds.isEmpty() ? Map.of() :
                propertyImageRepository.findByProperty_IdInAndCoverTrue(propertyIds).stream()
                        .collect(Collectors.toMap(
                                img -> img.getProperty().getId(),
                                PropertyImage::getUrl
                        ));

        // 5. Map to DTO
        return propertyPage.map(property -> PublicPropertyListItemResponse.builder()
                .id(property.getId())
                .title(property.getTitle())
                .purpose(property.getPurpose())
                .propertyType(property.getPropertyType())
                .price(property.getPrice())
                .currency(property.getCurrency())
                .priceUnit(property.getPriceUnit())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .sizeSqm(property.getSizeSqm())
                .province(property.getProvince())
                .district(property.getDistrict())
                .commune(property.getCommune())
                .coverImageUrl(coverImageUrls.get(property.getId()))
                .publishedAt(resolvePublishedAt(property))
                .build());
    }

    @Override
    @Transactional
    public void recordContactClick(Long propertyId) {
        Property property = propertyRepository.findByIdAndStatus(propertyId, com.roomkh.backend.entity.PropertyStatus.ACTIVE)
                .orElseThrow(() -> new com.roomkh.backend.exception.ResourceNotFoundException("Property not found or unavailable."));

        property.setInquiryCount((property.getInquiryCount() == null ? 0 : property.getInquiryCount()) + 1);
        // Hibernate's dirty checking will automatically flush this update to the database upon transaction commit.
    }

    @Override
    @Transactional(readOnly = true)
    public HomeDataResponse getHomeData() {
        // 1. Fetch Featured Properties (Limit 8, ordered by newest)
        Pageable featuredLimit = PageRequest.of(0, 8);
        List<Property> featuredEntities = propertyRepository.findByStatusOrderByCreatedAtDesc(
                PropertyStatus.ACTIVE, featuredLimit);

        // Fetch cover images in bulk to prevent N+1
        List<Long> propertyIds = featuredEntities.stream().map(Property::getId).toList();
        Map<Long, String> coverImageUrls = propertyIds.isEmpty() ? Map.of() :
                propertyImageRepository.findByProperty_IdInAndCoverTrue(propertyIds).stream()
                        .collect(Collectors.toMap(
                                img -> img.getProperty().getId(),
                                PropertyImage::getUrl
                        ));

        // Map featured properties
        List<PublicPropertyListItemResponse> featuredProperties = featuredEntities.stream()
                .map(property -> PublicPropertyListItemResponse.builder()
                        .id(property.getId())
                        .title(property.getTitle())
                        .purpose(property.getPurpose())
                        .propertyType(property.getPropertyType())
                        .price(property.getPrice())
                        .currency(property.getCurrency())
                        .priceUnit(property.getPriceUnit())
                        .bedrooms(property.getBedrooms())
                        .bathrooms(property.getBathrooms())
                        .sizeSqm(property.getSizeSqm())
                        .province(property.getProvince())
                        .district(property.getDistrict())
                        .commune(property.getCommune())
                        .coverImageUrl(coverImageUrls.get(property.getId()))
                        .publishedAt(resolvePublishedAt(property))
                        .build())
                .toList();

        // 2. Fetch Popular Locations (Limit 4, grouped by province)
        Pageable locationLimit = PageRequest.of(0, 4);
        List<Object[]> locationResults = propertyRepository.findPopularLocations(locationLimit);

        List<LocationSummaryResponse> locations = locationResults.stream()
                .map(row -> LocationSummaryResponse.builder()
                        .province((String) row[0])
                        .propertyCount((Long) row[1])
                        .build())
                .toList();

        // 3. Assemble and return response
        return HomeDataResponse.builder()
                .featuredProperties(featuredProperties)
                .locations(locations)
                .build();
    }

    private Sort resolveSort(String sortBy) {
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "price");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private OffsetDateTime resolvePublishedAt(Property property) {
        if (property.getListedAt() != null) return property.getListedAt();
        if (property.getUpdatedAt() != null) return property.getUpdatedAt();
        return property.getCreatedAt();
    }
}