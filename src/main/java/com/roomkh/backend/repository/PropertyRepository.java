package com.roomkh.backend.repository;

import com.roomkh.backend.entity.Property;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.PropertyType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    boolean existsBySlug(String slug);
    Page<Property> findByStatusNot(PropertyStatus status, Pageable pageable);
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

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Property p WHERE p.id = :id")
    Optional<Property> findByIdForUpdate(@Param("id") Long id);

    Optional<Property> findByIdAndStatus(Long id, PropertyStatus status);

    @Query("SELECT p FROM Property p WHERE p.status = 'ACTIVE' AND p.id != :propertyId AND p.propertyType = :type AND p.province = :province ORDER BY p.createdAt DESC")
    List<Property> findSimilarProperties(
            @Param("propertyId") Long propertyId,
            @Param("type") PropertyType type,
            @Param("province") String province,
            Pageable pageable);

    @Query("SELECT p.province, COUNT(p.id) FROM Property p WHERE p.status = com.roomkh.backend.entity.PropertyStatus.ACTIVE GROUP BY p.province ORDER BY COUNT(p.id) DESC")
    List<Object[]> findPopularLocations(Pageable pageable);

    List<Property> findByStatusOrderByCreatedAtDesc(PropertyStatus status, Pageable pageable);

    @Query("SELECT DISTINCT p.province FROM Property p WHERE p.status = 'ACTIVE' AND p.province IS NOT NULL")
    List<String> findDistinctProvincesWithActiveProperties();

    long countByStatus(com.roomkh.backend.entity.PropertyStatus status);

    @Query("SELECT p FROM Property p WHERE " +
            "(:search = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.province) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.seller.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:type IS NULL OR p.propertyType = :type) " +
            "AND (:city = '' OR LOWER(p.province) = LOWER(:city)) " +
            "AND (p.createdAt BETWEEN :startDate AND :endDate)")
    Page<Property> findPropertiesWithFilters(
            @Param("search") String search,
            @Param("status") PropertyStatus status,
            @Param("type") PropertyType type,
            @Param("city") String city,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    long countByStatusAndCreatedAtBetween(PropertyStatus status, OffsetDateTime start, OffsetDateTime end);

    long countBySellerId(Long sellerId);
}