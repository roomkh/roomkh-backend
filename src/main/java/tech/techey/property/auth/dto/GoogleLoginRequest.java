package tech.techey.property.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank @Size(max = 4096) String code,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_~.\\-]{43,128}") String codeVerifier,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String nonce,
        @NotBlank @Pattern(regexp = "OWNER|ADMIN") String portal) {
    @Override public String toString() {
        return "GoogleLoginRequest[credentials=REDACTED, portal=" + portal + "]";
    }
}
