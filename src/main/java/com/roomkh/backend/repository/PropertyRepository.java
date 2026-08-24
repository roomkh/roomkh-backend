package com.roomkh.backend.repository;

import com.roomkh.backend.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    boolean existsBySlug(String slug);
    Optional<Property> findBySlug(String slug);
    Optional<Property> findByIdAndSellerId(Long propertyId, Long sellerId);
}