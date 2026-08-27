package com.roomkh.backend.repository;

import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyPurpose;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.PropertyType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PropertySpecification {

    public static Specification<Property> filterPublicProperties(
            String purpose, 
            String propertyType, 
            BigDecimal minPrice, 
            BigDecimal maxPrice, 
            String province) {
            
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // CRITICAL: Always restrict to ACTIVE status
            predicates.add(criteriaBuilder.equal(root.get("status"), PropertyStatus.ACTIVE));

            if (purpose != null && !purpose.isBlank()) {
                try {
                    predicates.add(criteriaBuilder.equal(root.get("purpose"), PropertyPurpose.valueOf(purpose.trim().toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }

            if (propertyType != null && !propertyType.isBlank()) {
                try {
                    predicates.add(criteriaBuilder.equal(root.get("propertyType"), PropertyType.valueOf(propertyType.trim().toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (province != null && !province.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("province")), province.trim().toLowerCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}