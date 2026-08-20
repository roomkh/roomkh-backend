package com.roomkh.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Identifier is required.")
    private String identifier;

    @NotBlank(message = "Password is required.")
    private String password;

    private boolean rememberMe = false;
}