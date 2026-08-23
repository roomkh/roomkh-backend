package com.roomkh.backend.repository;

import com.roomkh.backend.entity.SellerRequestOtpCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface SellerRequestOtpCodeRepository extends JpaRepository<SellerRequestOtpCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM SellerRequestOtpCode o WHERE o.sellerRequest.id = :sellerRequestId " +
            "AND o.consumedAt IS NULL AND o.invalidatedAt IS NULL ORDER BY o.createdAt DESC")
    List<SellerRequestOtpCode> findActiveForUpdate(@Param("sellerRequestId") Long sellerRequestId);

    long countBySellerRequest_IdAndCreatedAtAfter(Long sellerRequestId, OffsetDateTime after);
}