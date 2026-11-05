package tech.techey.property.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(@NotBlank String phone) {
}
