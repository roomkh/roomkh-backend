package tech.techey.property.common.exception;

import lombok.Getter;

/**
 * Application-level exception carrying an {@link ErrorCode} and an optional offending field name.
 * Caught centrally by {@link GlobalExceptionHandler} and rendered as an {@code ApiResponse}.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String field;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public ApiException(ErrorCode errorCode, String field) {
        this(errorCode, field, null);
    }

    public ApiException(ErrorCode errorCode, String field, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.name());
        this.errorCode = errorCode;
        this.field = field;
    }

    public static ApiException of(ErrorCode errorCode) {
        return new ApiException(errorCode);
    }

    public static ApiException of(ErrorCode errorCode, String field) {
        return new ApiException(errorCode, field);
    }
}
