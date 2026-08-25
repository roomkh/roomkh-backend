package com.roomkh.backend.validation;

import com.roomkh.backend.entity.PriceUnit;
import com.roomkh.backend.entity.PropertyPurpose;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PropertyPriceUnitValidator implements ConstraintValidator<ValidPropertyPriceUnit, PropertyPriceFields> {

    @Override
    public boolean isValid(PropertyPriceFields request, ConstraintValidatorContext context) {
        if (request == null || request.getPurpose() == null || request.getPriceUnit() == null) {
            return true;
        }

        PropertyPurpose purpose = request.getPurpose();
        PriceUnit priceUnit = request.getPriceUnit();

        boolean valid = (purpose == PropertyPurpose.RENT && priceUnit == PriceUnit.MONTH)
                || (purpose == PropertyPurpose.SALE && priceUnit == PriceUnit.SELL);

        if (!valid) {
            context.disableDefaultConstraintViolation();
            String message = purpose == PropertyPurpose.RENT
                    ? "MONTH is required when purpose is RENT."
                    : "SELL is required when purpose is SALE.";
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("priceUnit")
                    .addConstraintViolation();
        }

        return valid;
    }
}