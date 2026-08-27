package com.roomkh.backend.dto.property;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderPropertyImagesRequest {

    @NotEmpty(message = "image_ids is required and must not be empty.")
    @Size(max = 10, message = "A property can have a maximum of 10 images.")
    private List<@NotNull(message = "Image ID must not be null.") @Positive(message = "Image ID must be positive.") Long> imageIds;
}