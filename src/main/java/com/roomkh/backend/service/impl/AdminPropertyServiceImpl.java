package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.AdminPropertyListItemResponse;
import com.roomkh.backend.dto.property.AdminPropertyReviewRequest;
import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyImage;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.PropertyImageRepository;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.AdminPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPropertyServiceImpl implements AdminPropertyService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;

    private final UserRepository userRepository; // Ensure this is injected

    @Override
    @Transactional
    public void reviewProperty(Long adminId, Long propertyId, AdminPropertyReviewRequest request) {
        // 1. Fetch admin user for auditing
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found."));

        // 2. Fetch and lock target property row
        Property property = propertyRepository.findByIdForUpdate(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found."));

        // 3. Validate state machine rule (Must be PENDING)
        if (property.getStatus() != PropertyStatus.PENDING) {
            throw new BadRequestException("Only PENDING properties can be reviewed.");
        }

        // 4. Validate requested status
        String requestedStatus = request.getStatus().trim().toUpperCase();
        if (!requestedStatus.equals(PropertyStatus.ACTIVE.name()) && !requestedStatus.equals(PropertyStatus.REJECTED.name())) {
            throw new BadRequestException("Invalid review status. Must be ACTIVE or REJECTED.");
        }

        // 5. Apply transitions and rejection reason validation
        if (requestedStatus.equals(PropertyStatus.REJECTED.name())) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("A rejection reason is strictly required when rejecting a property.");
            }
            property.setStatus(PropertyStatus.REJECTED);
            property.setRejectionReason(request.getRejectionReason().trim());
        } else {
            property.setStatus(PropertyStatus.ACTIVE);
            property.setRejectionReason(null); // Clear any previous rejection reason
        }

        // 6. Update audit fields
        property.setReviewedBy(adminUser);
        property.setReviewedAt(java.time.OffsetDateTime.now());

        // 7. Save entity
        propertyRepository.save(property);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPropertyListItemResponse> listProperties(String status, int page, int size, String sortBy) {
        if (page < 1) {
            throw new BadRequestException("page must be at least 1.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and 50.");
        }

        Sort sort = resolveSort(sortBy);
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        PropertyStatus parsedStatus = parseStatus(status);

        if (parsedStatus == PropertyStatus.DRAFT) {
            throw new BadRequestException("Admins are not allowed to view DRAFT properties.");
        }

        // 1. Fetch properties
        Page<Property> propertyPage = (parsedStatus != null)
                ? propertyRepository.findByStatus(parsedStatus, pageable)
                : propertyRepository.findByStatusNot(PropertyStatus.DRAFT, pageable);

        List<Long> propertyIds = propertyPage.getContent().stream()
                .map(Property::getId)
                .toList();

        // 2. Fetch cover images to avoid N+1 issues
        Map<Long, String> coverImageUrls = propertyIds.isEmpty() ? Map.of() : 
            propertyImageRepository.findByProperty_IdInAndCoverTrue(propertyIds).stream()
                .collect(Collectors.toMap(
                        img -> img.getProperty().getId(),
                        PropertyImage::getUrl
                ));

        // 3. Map to DTO
        return propertyPage.map(property -> {
            User seller = property.getSeller();
            return AdminPropertyListItemResponse.builder()
                    .id(property.getId())
                    .title(property.getTitle())
                    .status(property.getStatus())
                    .propertyType(property.getPropertyType())
                    .price(property.getPrice())
                    .currency(property.getCurrency())
                    .priceUnit(property.getPriceUnit())
                    .submittedAt(property.getSubmittedAt())
                    .createdAt(property.getCreatedAt())
                    .coverImageUrl(coverImageUrls.get(property.getId()))
                    .ownerId(seller != null ? seller.getId() : null)
                    .ownerName(seller != null ? seller.getFullName() : null)
                    .ownerEmail(seller != null ? seller.getEmail() : null)
                    .ownerPhone(seller != null ? seller.getPhoneNumber() : null)
                    .build();
        });
    }

    private Sort resolveSort(String sortBy) {
        if ("recently_submitted".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "submittedAt");
        }
        // Default to "newest"
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private PropertyStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PropertyStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value. Allowed values: DRAFT, PENDING, ACTIVE, REJECTED, SOLD_RENTED.");
        }
    }
}