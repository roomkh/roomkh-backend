package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.PropertyImageDeleteResponse;
import com.roomkh.backend.dto.property.PropertyImageOrderResponse;
import com.roomkh.backend.dto.property.PropertyImageUploadResponse;
import com.roomkh.backend.dto.property.ReorderPropertyImagesRequest;
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
import java.util.*;
import java.util.stream.Collectors;

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
    public List<PropertyImageOrderResponse> reorderImages(
            Long authenticatedUserId,
            Long propertyId,
            ReorderPropertyImagesRequest request
    ) {
        User seller = loadVerifiedSeller(authenticatedUserId);

        Property property = propertyRepository.findByIdAndSellerIdForUpdate(propertyId, seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found."));

        verifyEditableProperty(property);

        List<PropertyImage> existingImages = propertyImageRepository
                .findByProperty_IdOrderBySortOrderAsc(propertyId);

        List<Long> submittedImageIds = request.getImageIds();

        validateCompleteImageOrder(existingImages, submittedImageIds);

        Map<Long, PropertyImage> imagesById = existingImages.stream()
                .collect(Collectors.toMap(PropertyImage::getId, image -> image));

        /*
         * Phase 1:
         * Use unique temporary high positive sort-order values. Negative values are
         * not allowed because PostgreSQL enforces chk_property_images_sort_order_positive.
         *
         * The final valid sort orders are only 1 through the number of property images
         * (maximum 10), so values starting from 1000 cannot collide with final values.
         */
        final int temporarySortOrderBase = 1000;

        for (int index = 0; index < existingImages.size(); index++) {
            PropertyImage image = existingImages.get(index);

            image.setSortOrder(temporarySortOrderBase + index + 1);
            image.setCover(false);
        }

        propertyImageRepository.saveAll(existingImages);
        propertyImageRepository.flush();

        /*
         * Phase 2:
         * Apply the requested order. Position 0 becomes sort order 1 and the
         * only cover image. All other images become non-cover images.
         */
        List<PropertyImage> orderedImages = new java.util.ArrayList<>();

        for (int index = 0; index < submittedImageIds.size(); index++) {
            Long imageId = submittedImageIds.get(index);
            PropertyImage image = imagesById.get(imageId);

            image.setSortOrder(index + 1);
            image.setCover(index == 0);

            orderedImages.add(image);
        }

        propertyImageRepository.saveAll(orderedImages);
        propertyImageRepository.flush();

        return orderedImages.stream()
                .map(this::toPropertyImageOrderResponse)
                .toList();
    }

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

    private void validateCompleteImageOrder(
            List<PropertyImage> existingImages,
            List<Long> submittedImageIds
    ) {
        if (submittedImageIds.size() != existingImages.size()) {
            throw new BadRequestException("Image order must include all property images exactly once.");
        }

        Set<Long> submittedUniqueIds = new HashSet<>(submittedImageIds);

        if (submittedUniqueIds.size() != submittedImageIds.size()) {
            throw new BadRequestException("Image order must include all property images exactly once.");
        }

        Set<Long> existingImageIds = existingImages.stream()
                .map(PropertyImage::getId)
                .collect(Collectors.toSet());

        if (!existingImageIds.containsAll(submittedUniqueIds)) {
            throw new BadRequestException("One or more images do not belong to this property.");
        }

        if (!submittedUniqueIds.equals(existingImageIds)) {
            throw new BadRequestException("Image order must include all property images exactly once.");
        }
    }

    private PropertyImageOrderResponse toPropertyImageOrderResponse(PropertyImage image) {
        return PropertyImageOrderResponse.builder()
                .id(image.getId())
                .propertyId(image.getProperty().getId())
                .url(image.getUrl())
                .isCover(image.isCover())
                .sortOrder(image.getSortOrder())
                .build();
    }
}