package com.roomkh.backend.storage.impl;

import com.roomkh.backend.config.R2StorageProperties;
import com.roomkh.backend.storage.PropertyImageStorage;
import com.roomkh.backend.storage.StoredPropertyImage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "property-image-storage", name = "provider", havingValue = "r2")
@RequiredArgsConstructor
public class R2PropertyImageStorage implements PropertyImageStorage {

    private final R2StorageProperties r2Properties;
    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (r2Properties.getEndpoint() == null || r2Properties.getAccessKey() == null || r2Properties.getSecretKey() == null) {
            log.warn("Cloudflare R2 properties not fully set. S3Client initialization deferred.");
            return;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                r2Properties.getAccessKey(),
                r2Properties.getSecretKey()
        );

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(r2Properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1)
                .build();

        log.info("Initialized Cloudflare R2 S3Client for bucket: {}", r2Properties.getBucketName());
    }

    @Override
    public StoredPropertyImage store(MultipartFile image) {
        if (s3Client == null) {
            init();
            if (s3Client == null) {
                throw new IllegalStateException("Cloudflare R2 storage client is not initialized.");
            }
        }

        String extension = resolveExtension(image.getContentType());
        String generatedFileName = UUID.randomUUID() + extension;
        String objectKey = "properties/" + generatedFileName;

        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(objectKey)
                    .contentType(image.getContentType())
                    .build();

            s3Client.putObject(putOb, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));

            String publicUrl;
            if (r2Properties.getPublicUrl() != null && !r2Properties.getPublicUrl().isBlank()) {
                String baseUrl = r2Properties.getPublicUrl().replaceAll("/+$", "");
                publicUrl = baseUrl + "/" + objectKey;
            } else {
                publicUrl = r2Properties.getEndpoint() + "/" + r2Properties.getBucketName() + "/" + objectKey;
            }

            return new StoredPropertyImage(generatedFileName, publicUrl, image.getContentType(), image.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to upload image to Cloudflare R2.", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (s3Client == null) {
            return;
        }

        try {
            String objectKey = "properties/" + storageKey;
            DeleteObjectRequest deleteOb = DeleteObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteOb);
        } catch (Exception e) {
            log.error("Failed to delete property image {} from Cloudflare R2", storageKey, e);
        }
    }

    private String resolveExtension(String contentType) {
        if ("image/png".equalsIgnoreCase(contentType)) return ".png";
        if ("image/webp".equalsIgnoreCase(contentType)) return ".webp";
        if ("image/gif".equalsIgnoreCase(contentType)) return ".gif";
        return ".jpg";
    }
}
