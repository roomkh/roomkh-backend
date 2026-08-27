package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.PublicPropertyListItemResponse;
import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyImage;
import com.roomkh.backend.exception.BadRequestException;
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
    public Page<PublicPropertyListItemResponse> searchProperties(
            int page, int size, String purpose, String propertyType, 
            BigDecimal minPrice, BigDecimal maxPrice, String province, String sortBy) {
            
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
                purpose, propertyType, minPrice, maxPrice, province);

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