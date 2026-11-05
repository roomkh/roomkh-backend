package tech.techey.property.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tech.techey.property.common.dto.ApiError;
import tech.techey.property.common.dto.ApiResponse;

/**
 * Central place where every exception thrown by a controller/service is translated
 * into the standard {@link ApiResponse} envelope, with bilingual (km/en) messages
 * resolved from {@code MessageSource} based on {@code Accept-Language}.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toApiError)
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ApiResponse.error(errors));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ErrorCode code = ex.getErrorCode();
        String message = resolveMessage(code.getMessageKey(), ex.getMessage());
        ApiError error = new ApiError(code.name(), ex.getField(), message);
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(error));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        ErrorCode code = ErrorCode.INVALID_CREDENTIALS;
        ApiError error = new ApiError(code.name(), null, resolveMessage(code.getMessageKey(), ex.getMessage()));
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(error));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            org.springframework.security.core.AuthenticationException ex) {
        ErrorCode code = ErrorCode.UNAUTHORIZED;
        ApiError error = new ApiError(code.name(), null, resolveMessage(code.getMessageKey(), ex.getMessage()));
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode code = ErrorCode.FORBIDDEN;
        ApiError error = new ApiError(code.name(), null, resolveMessage(code.getMessageKey(), ex.getMessage()));
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(error));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        ErrorCode code = ErrorCode.NOT_FOUND;
        ApiError error = new ApiError(code.name(), null, resolveMessage(code.getMessageKey(), ex.getMessage()));
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(error));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ApiError error = new ApiError("METHOD_NOT_ALLOWED", null, ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        ApiError error = new ApiError(code.name(), null, resolveMessage(code.getMessageKey(), ex.getMessage()));
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(error));
    }

    private ApiError toApiError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : resolveMessage(ErrorCode.VALIDATION_ERROR.getMessageKey(), "Invalid value");
        return new ApiError(ErrorCode.VALIDATION_ERROR.name(), fieldError.getField(), message);
    }

    private String resolveMessage(String key, String fallback) {
        try {
            return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return fallback != null ? fallback : key;
        }
    }
}
