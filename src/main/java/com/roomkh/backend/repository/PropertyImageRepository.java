package com.roomkh.backend.repository;

import com.roomkh.backend.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    boolean existsByProperty_Id(Long propertyId);

    long countByProperty_Id(Long propertyId);

    Optional<PropertyImage> findByProperty_IdAndCoverTrue(Long propertyId);

    boolean existsByProperty_IdAndSortOrder(Long propertyId, Integer sortOrder);

    Optional<PropertyImage> findByIdAndProperty_Id(Long imageId, Long propertyId);

    Optional<PropertyImage> findFirstByProperty_IdOrderBySortOrderAsc(Long propertyId);

    List<PropertyImage> findByProperty_IdOrderBySortOrderAsc(Long propertyId);

    List<PropertyImage> findByProperty_Id(Long propertyId);

    List<PropertyImage> findByProperty_IdInAndCoverTrue(Collection<Long> propertyIds);

    @Query("SELECT MAX(pi.sortOrder) FROM PropertyImage pi WHERE pi.property.id = :propertyId")
    Optional<Integer> findMaxSortOrderByPropertyId(@Param("propertyId") Long propertyId);
}