package com.roomkh.backend.storage.impl;

import com.roomkh.backend.config.PropertyImageStorageProperties;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.storage.PropertyImageStorage;
import com.roomkh.backend.storage.StoredPropertyImage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(prefix = "property-image-storage", name = "provider", havingValue = "local")
@RequiredArgsConstructor
public class LocalPropertyImageStorage implements PropertyImageStorage {

    private final PropertyImageStorageProperties properties;

    @Override
    public StoredPropertyImage store(MultipartFile image) {
        String extension = resolveExtension(image.getContentType());
        String generatedFileName = UUID.randomUUID() + extension;

        try {
            Path baseDir = Paths.get(properties.getRootPath(), "properties").toAbsolutePath().normalize();
            Files.createDirectories(baseDir);

            Path targetFile = baseDir.resolve(generatedFileName).normalize();
            if (!targetFile.startsWith(baseDir)) {
                throw new BadRequestException("Invalid image file.");
            }

            image.transferTo(targetFile);

            String url = properties.getPublicUrlPrefix() + "/properties/" + generatedFileName;
            return new StoredPropertyImage(generatedFileName, url, image.getContentType(), image.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store property image.", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path baseDir = Paths.get(properties.getRootPath(), "properties").toAbsolutePath().normalize();
            Path targetFile = baseDir.resolve(storageKey).normalize();
            if (targetFile.startsWith(baseDir)) {
                Files.deleteIfExists(targetFile);
            }
        } catch (IOException e) {
            // Best-effort cleanup only; never fail the caller because of this.
        }
    }

    private String resolveExtension(String contentType) {
        return "image/png".equals(contentType) ? ".png" : ".jpg";
    }
}