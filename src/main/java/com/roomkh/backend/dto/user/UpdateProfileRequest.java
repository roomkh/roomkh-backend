package com.roomkh.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    
    @NotBlank(message = "Full name is required")
    @JsonProperty("full_name")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
}