package com.roomkh.backend.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StoredPropertyImage {
    private final String storageKey;
    private final String url;
    private final String contentType;
    private final long fileSize;
}