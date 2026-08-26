package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.PropertyImageDeleteResponse;
import com.roomkh.backend.dto.property.PropertyImageUploadResponse;
import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyImage;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.DuplicateResourceException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.exception.ServiceUnavailableException;
import com.roomkh.backend.repository.PropertyImageRepository;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.PropertyImageService;
import com.roomkh.backend.storage.PropertyImageStorage;
import com.roomkh.backend.storage.StoredPropertyImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyImageServiceImpl implements PropertyImageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_IMAGES_PER_PROPERTY = 10;

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final Optional<PropertyImageStorage> propertyImageStorage;

    @Override
    @Transactional
    public PropertyImageUploadResponse uploadImage(
            Long authenticatedUserId,
            Long propertyId,
            MultipartFile image,
            Boolean isCover,
            Integer sortOrder
    ) {
        User seller = loadVerifiedSeller(authenticatedUserId);

        Property property = propertyRepository.findByIdAndSellerIdForUpdate(propertyId, seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found."));

        verifyEditableProperty(property);

        long existingImageCount = propertyImageRepository.countByProperty_Id(propertyId);
        if (existingImageCount >= MAX_IMAGES_PER_PROPERTY) {
            throw new DuplicateResourceException("A property can have a maximum of 10 images.");
        }

        validateImageFile(image);

        if (sortOrder != null) {
            if (sortOrder <= 0) {
                throw new BadRequestException("sort_order must be greater than 0.");
            }

            if (propertyImageRepository.existsByProperty_IdAndSortOrder(propertyId, sortOrder)) {
                throw new DuplicateResourceException("Sort order is already in use for this property.");
            }
        }

        int finalSortOrder = sortOrder != null
                ? sortOrder
                : propertyImageRepository.findMaxSortOrderByPropertyId(propertyId).orElse(0) + 1;

        boolean isFirstImage = existingImageCount == 0;
        boolean finalIsCover = isFirstImage || Boolean.TRUE.equals(isCover);

        PropertyImageStorage storage = getStorage();
        StoredPropertyImage stored = storage.store(image);

        try {
            if (finalIsCover && !isFirstImage) {
                propertyImageRepository.findByProperty_IdAndCoverTrue(propertyId)
                        .ifPresent(existingCover -> {
                            existingCover.setCover(false);
                            propertyImageRepository.saveAndFlush(existingCover);
                        });
            }

            PropertyImage propertyImage = PropertyImage.builder()
                    .property(property)
                    .url(stored.getUrl())
                    .storageKey(stored.getStorageKey())
                    .contentType(stored.getContentType())
                    .fileSize(stored.getFileSize())
                    .cover(finalIsCover)
                    .sortOrder(finalSortOrder)
                    .build();

            PropertyImage saved = propertyImageRepository.saveAndFlush(propertyImage);

            return PropertyImageUploadResponse.builder()
                    .id(saved.getId())
                    .propertyId(property.getId())
                    .url(saved.getUrl())
                    .cover(saved.isCover())
                    .sortOrder(saved.getSortOrder())
                    .contentType(saved.getContentType())
                    .fileSize(saved.getFileSize())
                    .createdAt(saved.getCreatedAt())
                    .build();
        } catch (RuntimeException ex) {
            storage.delete(stored.getStorageKey());
            throw ex;
        }
    }

    @Override
    @Transactional
    public PropertyImageDeleteResponse deleteImage(
            Long authenticatedUserId,
            Long propertyId,
            Long imageId
    ) {
        User seller = loadVerifiedSeller(authenticatedUserId);

        Property property = propertyRepository.findByIdAndSellerIdForUpdate(propertyId, seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found."));

        verifyEditableProperty(property);

        PropertyImage image = propertyImageRepository.findByIdAndProperty_Id(imageId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property image not found."));

        boolean wasCover = image.isCover();
        String storageKey = image.getStorageKey();

        propertyImageRepository.delete(image);
        propertyImageRepository.flush();

        Long newCoverImageId = null;

        if (wasCover) {
            Optional<PropertyImage> nextImage = propertyImageRepository
                    .findFirstByProperty_IdOrderBySortOrderAsc(propertyId);

            if (nextImage.isPresent()) {
                PropertyImage newCover = nextImage.get();
                newCover.setCover(true);
                propertyImageRepository.saveAndFlush(newCover);
                newCoverImageId = newCover.getId();
            }
        }

        registerAfterCommitStorageDeletion(storageKey);

        return PropertyImageDeleteResponse.builder()
                .id(imageId)
                .propertyId(propertyId)
                .wasCover(wasCover)
                .newCoverImageId(newCoverImageId)
                .build();
    }

    private User loadVerifiedSeller(Long authenticatedUserId) {
        User seller = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        if (seller.getRole().getName() != RoleName.SELLER) {
            throw new BadRequestException("Only SELLER accounts can manage properties.");
        }

        return seller;
    }

    private void verifyEditableProperty(Property property) {
        if (property.getStatus() != PropertyStatus.DRAFT && property.getStatus() != PropertyStatus.REJECTED) {
            throw new DuplicateResourceException("Only DRAFT or REJECTED properties can be modified.");
        }
    }

    private PropertyImageStorage getStorage() {
        return propertyImageStorage.orElseThrow(
                () -> new ServiceUnavailableException("Property image storage is not configured.")
        );
    }

    private void registerAfterCommitStorageDeletion(String storageKey) {
        PropertyImageStorage storage = getStorage();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storage.delete(storageKey);
                } catch (RuntimeException ex) {
                    log.warn("Property image file cleanup failed after database deletion.");
                }
            }
        });
    }

    private void validateImageFile(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Image file is required.");
        }

        if (image.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Image file must not exceed 5 MB.");
        }

        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new BadRequestException("Only JPG, JPEG, and PNG images are allowed.");
        }

        String detectedFormat;

        try (InputStream inputStream = image.getInputStream()) {
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);

            if (imageInputStream == null) {
                throw new BadRequestException("Invalid image file.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                throw new BadRequestException("Invalid image file.");
            }

            ImageReader reader = readers.next();
            detectedFormat = reader.getFormatName();
            reader.setInput(imageInputStream);
            reader.read(0);
            reader.dispose();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new BadRequestException("Invalid image file.");
        }

        boolean validFormat = detectedFormat != null
                && (detectedFormat.equalsIgnoreCase("JPEG") || detectedFormat.equalsIgnoreCase("PNG"));

        if (!validFormat) {
            throw new BadRequestException("Invalid image file.");
        }
    }
}