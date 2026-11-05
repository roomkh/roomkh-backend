package tech.techey.property.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for verifying a phone OTP code. {@link tech.techey.property.auth.OtpService}
 * is a logging stub in this milestone — real SMS delivery is out of scope.
 */
public record OtpRequest(
        @NotBlank String phone,
        @NotBlank @Pattern(regexp = "^[0-9]{4,8}$") String code) {
}
