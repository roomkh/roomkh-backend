package com.roomkh.backend.validation;

import com.roomkh.backend.dto.property.CreatePropertyRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CoordinatesValidator implements ConstraintValidator<ValidCoordinates, CreatePropertyRequest> {

    @Override
    public boolean isValid(CreatePropertyRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        boolean bothNull = request.getLatitude() == null && request.getLongitude() == null;
        boolean bothPresent = request.getLatitude() != null && request.getLongitude() != null;

        if (bothNull || bothPresent) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Latitude and longitude must both be provided or both be null.")
                .addPropertyNode("latitude")
                .addConstraintViolation();
        return false;
    }
}