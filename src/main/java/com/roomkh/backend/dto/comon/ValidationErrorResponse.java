package com.roomkh.backend.dto.comon;

import jakarta.validation.ConstraintViolation;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ValidationErrorResponse {

    private ValidationErrorResponse() {
    }

    public static Map<String, String> toFieldErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return errors;
    }

    public static Map<String, String> toFieldErrors(Set<ConstraintViolation<?>> violations) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : violations) {
            String field = violation.getPropertyPath().toString();
            errors.put(field, violation.getMessage());
        }
        return errors;
    }
}