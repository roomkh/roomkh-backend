package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.PublicPropertyListItemResponse;
import com.roomkh.backend.dto.property.ToggleSaveResponse;
import com.roomkh.backend.entity.*;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.PropertyImageRepository;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.SavedPropertyRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.SavedPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedPropertyServiceImpl implements SavedPropertyService {

    private final SavedPropertyRepository savedPropertyRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

     private final PropertyImageRepository propertyImageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PublicPropertyListItemResponse> getSavedProperties(Long userId, int page, int size) {
        if (page < 1) throw new IllegalArgumentException("page must be at least 1.");

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<SavedProperty> savedPropertiesPage = savedPropertyRepository.findByUser_IdOrderBySavedAtDesc(userId, pageable);

        // Extract properties from the saved records
        List<Property> properties = savedPropertiesPage.getContent().stream()
                .map(SavedProperty::getProperty)
                .toList();

        // Fetch cover images in bulk to prevent N+1
        List<Long> propertyIds = properties.stream().map(Property::getId).toList();
        Map<Long, String> coverImageUrls = propertyIds.isEmpty() ? Map.of() :
                propertyImageRepository.findByProperty_IdInAndCoverTrue(propertyIds).stream()
                        .collect(Collectors.toMap(
                                img -> img.getProperty().getId(),
                                PropertyImage::getUrl
                        ));

        // Map to standard PublicPropertyListItemResponse DTO
        return savedPropertiesPage.map(saved -> {
            Property property = saved.getProperty();
            return PublicPropertyListItemResponse.builder()
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
                    .publishedAt(property.getCreatedAt())
                    .build();
        });
    }

    @Override
    @Transactional
    public ToggleSaveResponse toggleSaveProperty(Long userId, Long propertyId) {
        // 1. Fetch User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Validate Property exists and is ACTIVE
        Property property = propertyRepository.findByIdAndStatus(propertyId, PropertyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found or unavailable"));

        // 3. Check if already saved
        Optional<SavedProperty> existingSave = savedPropertyRepository.findByUser_IdAndProperty_Id(userId, propertyId);

        boolean isSaved;

        if (existingSave.isPresent()) {
            // 4. If exists -> Unsave (Delete)
            savedPropertyRepository.delete(existingSave.get());
            isSaved = false;
        } else {
            // 5. If doesn't exist -> Save (Create)
            SavedProperty newSave = SavedProperty.builder()
                    .user(user)
                    .property(property)
                    .savedAt(OffsetDateTime.now())
                    .build();
            savedPropertyRepository.save(newSave);
            isSaved = true;
        }

        // 6. Return response
        return ToggleSaveResponse.builder()
                .propertyId(propertyId)
                .isSaved(isSaved)
                .build();
    }
}