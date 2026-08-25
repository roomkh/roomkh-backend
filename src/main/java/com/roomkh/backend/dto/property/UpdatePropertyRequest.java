package com.roomkh.backend.dto.property;

import com.roomkh.backend.entity.PriceUnit;
import com.roomkh.backend.entity.PropertyPurpose;
import com.roomkh.backend.entity.PropertyType;
import com.roomkh.backend.validation.PropertyCoordinateFields;
import com.roomkh.backend.validation.PropertyPriceFields;
import com.roomkh.backend.validation.ValidCoordinates;
import com.roomkh.backend.validation.ValidPropertyPriceUnit;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ValidPropertyPriceUnit
@ValidCoordinates
public class UpdatePropertyRequest implements PropertyPriceFields, PropertyCoordinateFields {

    @NotBlank(message = "Title is required.")
    @Size(max = 255, message = "Title must not exceed 255 characters.")
    private String title;

    @NotNull(message = "Property type is required.")
    private PropertyType propertyType;

    @NotNull(message = "Purpose is required.")
    private PropertyPurpose purpose;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0.")
    @Digits(integer = 12, fraction = 2, message = "Price must have at most 2 decimal places.")
    private BigDecimal price;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must contain exactly 3 letters.")
    private String currency;

    @NotNull(message = "Price unit is required.")
    private PriceUnit priceUnit;

    @NotBlank(message = "Province is required.")
    @Size(max = 150, message = "Province must not exceed 150 characters.")
    private String province;

    @NotBlank(message = "District is required.")
    @Size(max = 150, message = "District must not exceed 150 characters.")
    private String district;

    @NotBlank(message = "Commune is required.")
    @Size(max = 150, message = "Commune must not exceed 150 characters.")
    private String commune;

    @Size(max = 500, message = "Address must not exceed 500 characters.")
    private String address;

    @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90.")
    @DecimalMax(value = "90", message = "Latitude must be between -90 and 90.")
    private BigDecimal latitude;

    @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180.")
    @DecimalMax(value = "180", message = "Longitude must be between -180 and 180.")
    private BigDecimal longitude;

    @NotBlank(message = "Description is required.")
    @Size(min = 20, max = 10000, message = "Description must be between 20 and 10000 characters.")
    private String description;

    @Min(value = 0, message = "Bedrooms cannot be negative.")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms cannot be negative.")
    private Integer bathrooms;

    @DecimalMin(value = "0.0", inclusive = false, message = "Size (sqm) must be greater than 0.")
    private BigDecimal sizeSqm;

    @Min(value = 0, message = "Floor cannot be negative.")
    private Integer floor;

    private boolean furnished;

    @Min(value = 0, message = "Age (years) cannot be negative.")
    private Integer ageYears;

    @NotNull(message = "amenity_codes is required. Send an empty array to remove all amenities.")
    private List<String> amenityCodes;

    private String status;
}