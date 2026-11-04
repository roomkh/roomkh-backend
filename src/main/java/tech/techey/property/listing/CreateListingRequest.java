package tech.techey.property.listing;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequest(
        @NotNull UUID id,
        @NotBlank @Size(max = 160) String title,
        @NotNull @Pattern(regexp = "RENT|SALE") String purpose,
        @NotNull @Positive Long propertyTypeId,
        @NotNull @Positive Long provinceId,
        @NotBlank @Size(max = 500) String address,
        @NotNull @Min(0) @Max(100) Integer bedrooms,
        @NotNull @Min(0) @Max(100) Integer bathrooms,
        @NotNull @DecimalMin("0.01") @DecimalMax("999999999999.99") @Digits(integer = 12, fraction = 2) BigDecimal area,
        @NotNull @DecimalMin("0.01") @DecimalMax("999999999999.99") @Digits(integer = 12, fraction = 2) BigDecimal price,
        @NotNull @Pattern(regexp = "USD|KHR") String currency,
        @NotBlank @Size(min = 20, max = 10000) String description,
        @NotBlank @Pattern(regexp = "^[0-9+][0-9 ]{7,20}$") String contactPhone,
        @NotNull @Size(max = 500) @Pattern(regexp = "(WIFI|PARKING|AIR_CONDITIONING|FURNISHED|BALCONY|ELEVATOR)(,(WIFI|PARKING|AIR_CONDITIONING|FURNISHED|BALCONY|ELEVATOR))*|^$") String amenities,
        @NotNull @Pattern(regexp = "DRAFT|PENDING_REVIEW") String status) {}
