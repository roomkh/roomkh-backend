package com.roomkh.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body of POST /api/v1/users/me/password. Same length rule as registration. */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required.")
    private String currentPassword;

    @NotBlank(message = "New password is required.")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required.")
    private String confirmPassword;
}
