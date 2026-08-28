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
            String location) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

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

            if (location != null && !location.isBlank()) {
                String searchPattern = "%" + location.trim().toLowerCase() + "%";
                Predicate provinceMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("province")), searchPattern);
                Predicate districtMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("district")), searchPattern);
                Predicate communeMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("commune")), searchPattern);

                predicates.add(criteriaBuilder.or(provinceMatch, districtMatch, communeMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}