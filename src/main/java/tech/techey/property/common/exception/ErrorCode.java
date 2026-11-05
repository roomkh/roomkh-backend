package tech.techey.property.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Starter set of machine-readable error codes returned in {@link tech.techey.property.common.dto.ApiError}.
 * The {@code messageKey} maps to a key in {@code messages_en.properties} / {@code messages_km.properties}.
 */
public enum ErrorCode {

    GOOGLE_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "error.google_not_configured"),
    GOOGLE_ACCOUNT_CONFLICT(HttpStatus.CONFLICT, "error.google_account_conflict"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "error.validation"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "error.forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "error.not_found"),
    CONFLICT(HttpStatus.CONFLICT, "error.conflict"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.invalid_credentials"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "error.token_expired"),
    TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "error.token_reuse_detected");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
