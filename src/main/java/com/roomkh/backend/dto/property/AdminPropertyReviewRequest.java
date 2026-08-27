package com.roomkh.backend.dto.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPropertyReviewRequest {

    @NotBlank(message = "Status is required.")
    private String status;

    @JsonProperty("rejection_reason")
    private String rejectionReason;
}