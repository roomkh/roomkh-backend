package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.property.ToggleSaveResponse;
import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.SavedProperty;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.SavedPropertyRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.SavedPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SavedPropertyServiceImpl implements SavedPropertyService {

    private final SavedPropertyRepository savedPropertyRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

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