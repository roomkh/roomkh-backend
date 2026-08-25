package com.roomkh.backend.repository;

import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    boolean existsBySlug(String slug);
    Optional<Property> findBySlug(String slug);
    Optional<Property> findByIdAndSellerId(Long propertyId, Long sellerId);

    Page<Property> findBySeller_Id(Long sellerId, Pageable pageable);
    Page<Property> findBySeller_IdAndStatus(Long sellerId, PropertyStatus status, Pageable pageable);

    long countBySeller_Id(Long sellerId);
    long countBySeller_IdAndStatus(Long sellerId, PropertyStatus status);
}