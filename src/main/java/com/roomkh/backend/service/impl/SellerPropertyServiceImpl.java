package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.CreatePropertyRequest;
import com.roomkh.backend.dto.property.SellerPropertyResponse;
import com.roomkh.backend.entity.Amenity;
import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.mapper.PropertyMapper;
import com.roomkh.backend.repository.AmenityRepository;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.SellerPropertyService;
import com.roomkh.backend.service.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerPropertyServiceImpl implements SellerPropertyService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyMapper propertyMapper;
    private final SlugGenerator slugGenerator;

    @Override
    @Transactional
    public SellerPropertyResponse createDraft(Long authenticatedUserId, CreatePropertyRequest request) {
        User seller = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        if (seller.getRole().getName() != RoleName.SELLER) {
            throw new BadRequestException("Only SELLER accounts can create properties.");
        }

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