package com.roomkh.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class LocalPropertyImageResourceConfig implements WebMvcConfigurer {

    private final PropertyImageStorageProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + properties.getRootPath() + "/";
        registry.addResourceHandler(properties.getPublicUrlPrefix() + "/**")
                .addResourceLocations(location);
    }
}