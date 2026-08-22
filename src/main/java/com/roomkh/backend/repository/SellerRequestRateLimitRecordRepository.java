package com.roomkh.backend.repository;

import com.roomkh.backend.entity.SellerRequestRateLimitKeyType;
import com.roomkh.backend.entity.SellerRequestRateLimitRecord;
import com.roomkh.backend.entity.SellerRequestRateLimitWindowType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SellerRequestRateLimitRecordRepository extends JpaRepository<SellerRequestRateLimitRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SellerRequestRateLimitRecord r WHERE r.keyType = :keyType AND r.keyHash = :keyHash AND r.windowType = :windowType")
    Optional<SellerRequestRateLimitRecord> findForUpdate(@Param("keyType") SellerRequestRateLimitKeyType keyType,
                                                          @Param("keyHash") String keyHash,
                                                          @Param("windowType") SellerRequestRateLimitWindowType windowType);
}