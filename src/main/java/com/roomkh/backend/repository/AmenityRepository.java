package com.roomkh.backend.repository;

import com.roomkh.backend.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    Optional<Amenity> findByCode(String code);
    List<Amenity> findByCodeIn(Collection<String> codes);
    boolean existsByCode(String code);
}