package com.roomkh.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    
    @NotBlank(message = "Google ID token is required")
    @JsonProperty("id_token")
    @JsonAlias({"idToken", "credential"})
    private String idToken;
    
}