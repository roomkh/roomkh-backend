package com.roomkh.backend.repository;

import com.roomkh.backend.entity.SavedProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Long> {
    Optional<SavedProperty> findByUser_IdAndProperty_Id(Long userId, Long propertyId);

    Page<SavedProperty> findByUser_IdOrderBySavedAtDesc(Long userId, Pageable pageable);
}