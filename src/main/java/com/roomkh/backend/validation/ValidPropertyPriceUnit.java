package com.roomkh.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PropertyPriceUnitValidator.class)
public @interface ValidPropertyPriceUnit {
    String message() default "Price unit does not match the selected purpose.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}