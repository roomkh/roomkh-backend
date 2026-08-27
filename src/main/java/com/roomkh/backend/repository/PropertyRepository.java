package com.roomkh.backend.repository;

import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    boolean existsBySlug(String slug);
    Optional<Property> findBySlug(String slug);
    Optional<Property> findByIdAndSellerId(Long propertyId, Long sellerId);

    Page<Property> findBySeller_Id(Long sellerId, Pageable pageable);
    Page<Property> findBySeller_IdAndStatus(Long sellerId, PropertyStatus status, Pageable pageable);

    long countBySeller_Id(Long sellerId);
    long countBySeller_IdAndStatus(Long sellerId, PropertyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Property p WHERE p.id = :propertyId AND p.seller.id = :sellerId")
    Optional<Property> findByIdAndSellerIdForUpdate(@Param("propertyId") Long propertyId, @Param("sellerId") Long sellerId);

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM Property p WHERE p.seller.id = :sellerId")
    Long sumViewCountBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT COALESCE(SUM(p.inquiryCount), 0) FROM Property p WHERE p.seller.id = :sellerId")
    Long sumInquiryCountBySellerId(@Param("sellerId") Long sellerId);

    // JpaRepository already includes Page<Property> findAll(Pageable pageable);

    Page<Property> findByStatus(PropertyStatus status, Pageable pageable);
}