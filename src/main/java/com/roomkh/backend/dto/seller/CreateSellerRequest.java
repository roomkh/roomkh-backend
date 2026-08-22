package com.roomkh.backend.dto.seller;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSellerRequest {

    @NotBlank(message = "Full name is required.")
    private String fullName;

    @Email(message = "Email must be a valid email address.")
    private String email;

    @NotBlank(message = "Phone number is required.")
    private String phoneNumber;

    @NotBlank(message = "Position is required.")
    private String position;

    private String businessName;

    @NotBlank(message = "Reason is required.")
    private String reason;

    @AssertTrue(message = "You must accept the terms and conditions.")
    private boolean termsAccepted;
}