package com.roomkh.backend.storage;

import org.springframework.web.multipart.MultipartFile;

public interface PropertyImageStorage {
    StoredPropertyImage store(MultipartFile image);
    void delete(String storageKey);
}