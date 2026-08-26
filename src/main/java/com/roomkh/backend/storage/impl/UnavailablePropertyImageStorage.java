package com.roomkh.backend.storage.impl;

import com.roomkh.backend.exception.ServiceUnavailableException;
import com.roomkh.backend.storage.PropertyImageStorage;
import com.roomkh.backend.storage.StoredPropertyImage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "property-image-storage", name = "provider", havingValue = "unavailable")
public class UnavailablePropertyImageStorage implements PropertyImageStorage {

    @Override
    public StoredPropertyImage store(MultipartFile image) {
        throw new ServiceUnavailableException("Property image storage is not configured.");
    }

    @Override
    public void delete(String storageKey) {
        // Nothing was ever stored, so there is nothing to delete.
    }
}