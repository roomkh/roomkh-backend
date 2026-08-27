package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.*;
import com.roomkh.backend.entity.*;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.DuplicateResourceException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.mapper.PropertyMapper;
import com.roomkh.backend.repository.AmenityRepository;
import com.roomkh.backend.repository.PropertyImageRepository;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.SellerPropertyListResult;
import com.roomkh.backend.service.SellerPropertyService;
import com.roomkh.backend.service.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerPropertyServiceImpl implements SellerPropertyService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyMapper propertyMapper;
    private final SlugGenerator slugGenerator;
    private final PropertyImageRepository propertyImageRepository;

    @Override
    @Transactional
    public SellerPropertyResponse submitPropertyForReview(Long authenticatedUserId, Long propertyId) {
        User seller = loadVerifiedSeller(authenticatedUserId);

        // 1. Lock property for update to prevent concurrent submissions
        Property property = propertyRepository.findByIdAndSellerIdForUpdate(propertyId, seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found."));

        // 2. Validate current status
        PropertyStatus currentStatus = property.getStatus();
        if (currentStatus == PropertyStatus.PENDING) {
            throw new BadRequestException("Property is already pending review.");
        }
        if (currentStatus == PropertyStatus.ACTIVE) {
            throw new BadRequestException("Active property cannot be submitted for review.");
        }
        if (currentStatus == PropertyStatus.SOLD_RENTED) {
            throw new BadRequestException("Sold or rented property cannot be submitted for review.");
        }

        // 3. Validate image constraints
        List<PropertyImage> images = propertyImageRepository.findByProperty_Id(propertyId);
        if (images == null || images.isEmpty()) {
            throw new BadRequestException("Property must have at least one image before submission.");
        }

        boolean hasCoverImage = images.stream().anyMatch(PropertyImage::isCover);
        if (!hasCoverImage) {
            throw new BadRequestException("Property must have a cover image assigned before submission.");
        }

        // 4. Update status and timestamp
        property.setStatus(PropertyStatus.PENDING);
        property.setSubmittedAt(OffsetDateTime.now());

        Property saved = propertyRepository.save(property);
        return propertyMapper.toSellerPropertyResponse(saved);
    }

    @Override
    @Transactional
    public SellerPropertyResponse createDraft(Long authenticatedUserId, CreatePropertyRequest request) {
        User seller = loadVerifiedSeller(authenticatedUserId);

        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        Set<Amenity> amenities = resolveAmenities(request.getAmenityCodes());
        String slug = slugGenerator.generateUniqueSlug(request.getTitle());

        Property property = Property.builder()
                .seller(seller)
                .title(request.getTitle().trim())
                .slug(slug)
                .purpose(request.getPurpose())
                .propertyType(request.getPropertyType())
                .price(request.getPrice())
                .currency(normalizedCurrency)
                .priceUnit(request.getPriceUnit())
                .status(PropertyStatus.DRAFT)
                .description(request.getDescription().trim())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .sizeSqm(request.getSizeSqm())
                .floor(request.getFloor())
                .furnished(request.isFurnished())
                .ageYears(request.getAgeYears())
                .address(request.getAddress())
                .province(request.getProvince().trim())
                .district(request.getDistrict().trim())
                .commune(request.getCommune().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .featured(false)
                .viewCount(0L)
                .inquiryCount(0L)
                .rejectionReason(null)
                .reviewedAt(null)
                .reviewedBy(null)
                .listedAt(null)
                .amenities(amenities)
                .build();

        Property saved = propertyRepository.save(property);
        return propertyMapper.toSellerPropertyResponse(saved);
    }

    @Override
    public SellerPropertyListResult listProperties(
            Long authenticatedUserId,
            PropertyStatus status,
            int page,
            int size,
            String sortBy
    ) {
        User seller = loadVerifiedSeller(authenticatedUserId);

        if (page < 1) {
            throw new BadRequestException("page must be at least 1.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and 50.");
        }

        Sort sort = resolveSort(sortBy);
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<Property> propertyPage = status != null
                ? propertyRepository.findBySeller_IdAndStatus(seller.getId(), status, pageable)
                : propertyRepository.findBySeller_Id(seller.getId(), pageable);

        List<Long> propertyIds = propertyPage.getContent().stream()
                .map(Property::getId)
                .toList();

        Map<Long, String> coverImageUrlsByPropertyId = propertyIds.isEmpty()
                ? Map.of()
                : propertyImageRepository.findByProperty_IdInAndCoverTrue(propertyIds).stream()
                .collect(Collectors.toMap(
                        propertyImage -> propertyImage.getProperty().getId(),
                        PropertyImage::getUrl
                ));

        Page<SellerPropertyListItemResponse> mappedPage = propertyPage.map(property ->
                propertyMapper.toSellerPropertyListItemResponse(
                        property,
                        coverImageUrlsByPropertyId.get(property.getId())
                )
        );

        SellerPropertyStatusCountsResponse statusCounts = buildStatusCounts(seller.getId());

        return new SellerPropertyListResult(mappedPage, statusCounts);
    }

    @Override
    @Transactional
    public SellerPropertyResponse updateProperty(Long authenticatedUserId, Long propertyId, UpdatePropertyRequest request) {
        User seller = loadVerifiedSeller(authenticatedUserId);

        Property property = propertyRepository.findByIdAndSellerId(propertyId, seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found."));

        if (property.getStatus() != PropertyStatus.DRAFT && property.getStatus() != PropertyStatus.REJECTED) {
            throw new DuplicateResourceException("Only DRAFT or REJECTED properties can be updated.");
        }

        validateRequestedStatus(request.getStatus(), property.getStatus());

        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        Set<Amenity> amenities = resolveAmenities(request.getAmenityCodes());

        String trimmedTitle = request.getTitle().trim();
        if (!property.getTitle().equals(trimmedTitle)) {
            String newSlug = slugGenerator.generateUniqueSlug(trimmedTitle);
            property.setSlug(newSlug);
        }

        property.setTitle(trimmedTitle);
        property.setPurpose(request.getPurpose());
        property.setPropertyType(request.getPropertyType());
        property.setPrice(request.getPrice());
        property.setCurrency(normalizedCurrency);
        property.setPriceUnit(request.getPriceUnit());
        property.setDescription(request.getDescription().trim());
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setSizeSqm(request.getSizeSqm());
        property.setFloor(request.getFloor());
        property.setFurnished(request.isFurnished());
        property.setAgeYears(request.getAgeYears());
        property.setAddress(request.getAddress());
        property.setProvince(request.getProvince().trim());
        property.setDistrict(request.getDistrict().trim());
        property.setCommune(request.getCommune().trim());
        property.setLatitude(request.getLatitude());
        property.setLongitude(request.getLongitude());

        property.getAmenities().clear();
        property.getAmenities().addAll(amenities);

        Property saved = propertyRepository.save(property);
        return propertyMapper.toSellerPropertyResponse(saved);
    }

    private void validateRequestedStatus(String requestedStatus, PropertyStatus currentStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            return;
        }
        String normalized = requestedStatus.trim().toUpperCase();
        if (!normalized.equals(currentStatus.name())) {
            throw new BadRequestException(
                    "Property status cannot be changed through this endpoint. It must remain " + currentStatus.name() + "."
            );
        }
    }

    private User loadVerifiedSeller(Long authenticatedUserId) {
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        if (user.getRole().getName() != RoleName.SELLER) {
            throw new BadRequestException("Only SELLER accounts can manage properties.");
        }

        return user;
    }

    private Sort resolveSort(String sortBy) {
        SellerPropertySortBy resolved;
        try {
            resolved = SellerPropertySortBy.valueOf(sortBy.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid sort_by value. Allowed values: recently_updated, newest, price_asc, price_desc.");
        }

        return switch (resolved) {
            case RECENTLY_UPDATED -> Sort.by(Sort.Direction.DESC, "updatedAt");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "updatedAt"));
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "updatedAt"));
        };
    }

    private SellerPropertyStatusCountsResponse buildStatusCounts(Long sellerId) {
        return SellerPropertyStatusCountsResponse.builder()
                .all(propertyRepository.countBySeller_Id(sellerId))
                .active(propertyRepository.countBySeller_IdAndStatus(sellerId, PropertyStatus.ACTIVE))
                .pending(propertyRepository.countBySeller_IdAndStatus(sellerId, PropertyStatus.PENDING))
                .draft(propertyRepository.countBySeller_IdAndStatus(sellerId, PropertyStatus.DRAFT))
                .soldRented(propertyRepository.countBySeller_IdAndStatus(sellerId, PropertyStatus.SOLD_RENTED))
                .build();
    }

    private String normalizeCurrency(String rawCurrency) {
        if (rawCurrency == null || rawCurrency.isBlank()) {
            return "USD";
        }
        return rawCurrency.trim().toUpperCase();
    }

    private Set<Amenity> resolveAmenities(List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<String> normalizedCodes = rawCodes.stream()
                .map(code -> code == null ? "" : code.trim().toUpperCase())
                .collect(Collectors.toList());

        if (normalizedCodes.stream().anyMatch(String::isBlank)) {
            throw new BadRequestException("Amenity codes must not be blank.");
        }

        Set<String> uniqueCodes = new LinkedHashSet<>(normalizedCodes);
        if (uniqueCodes.size() != normalizedCodes.size()) {
            throw new BadRequestException("Duplicate amenity codes are not allowed.");
        }

        List<Amenity> foundAmenities = amenityRepository.findByCodeIn(uniqueCodes);

        if (foundAmenities.size() != uniqueCodes.size()) {
            Set<String> foundCodes = foundAmenities.stream().map(Amenity::getCode).collect(Collectors.toSet());
            String missing = uniqueCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .collect(Collectors.joining(", "));
            throw new BadRequestException("Unknown amenity code(s): " + missing);
        }

        return new LinkedHashSet<>(foundAmenities);
    }
}