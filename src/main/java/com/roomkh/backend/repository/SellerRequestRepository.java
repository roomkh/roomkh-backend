package com.roomkh.backend.repository;

import com.roomkh.backend.entity.SellerRequest;
import com.roomkh.backend.entity.SellerRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SellerRequestRepository extends JpaRepository<SellerRequest, Long>, JpaSpecificationExecutor<SellerRequest> {
    boolean existsByUser_IdAndStatus(Long userId, SellerRequestStatus status);
    boolean existsByPhoneNumberAndStatus(String phoneNumber, SellerRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM SellerRequest sr WHERE sr.id = :id")
    Optional<SellerRequest> findByIdForUpdate(@Param("id") Long id);
}