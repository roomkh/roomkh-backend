package com.roomkh.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "property-image-storage")
public class PropertyImageStorageProperties {
    private String provider;
    private String rootPath;
    private String publicUrlPrefix;
}