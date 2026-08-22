package com.roomkh.backend.repository;

import com.roomkh.backend.entity.SellerRequest;
import com.roomkh.backend.entity.SellerRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SellerRequestRepository extends JpaRepository<SellerRequest, Long>, JpaSpecificationExecutor<SellerRequest> {
    boolean existsByUser_IdAndStatus(Long userId, SellerRequestStatus status);
    boolean existsByPhoneNumberAndStatus(String phoneNumber, SellerRequestStatus status);
}