package com.roomkh.backend.dto.property;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertyStatusRequest {

    @NotBlank(message = "Status is required.")
    private String status;
}