package tech.techey.property.common.dto;

/**
 * A single error item in an {@link ApiResponse}.
 *
 * @param code    machine-readable error code, e.g. {@code VALIDATION_ERROR}
 * @param field   offending field name, or null when not field-specific
 * @param message human-readable, locale-resolved message
 */
public record ApiError(String code, String field, String message) {
}
