package tech.techey.property.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code accountType} is optional: omitted means a plain seeker ({@code USER}).
 * Only {@code OWNER} or {@code AGENT} may be self-selected — privileged roles
 * (MODERATOR, ADMIN) are never assignable through public signup.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @Pattern(regexp = "^[0-9+][0-9 ]{7,20}$", message = "{error.validation}") String phone,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Pattern(regexp = "^(OWNER|AGENT)$", message = "{error.validation}") String accountType) {
}
