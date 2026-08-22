package com.roomkh.backend.service;

import com.roomkh.backend.dto.seller.CreateSellerRequest;
import com.roomkh.backend.dto.seller.SellerRequestResponse;
import com.roomkh.backend.dto.seller.SellerRequestSummaryResponse;
import com.roomkh.backend.entity.SellerRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SellerRequestService {
    SellerRequestResponse submit(CreateSellerRequest request, Long authenticatedUserId);
    Page<SellerRequestSummaryResponse> list(SellerRequestStatus status, String keyword, Pageable pageable);
    SellerRequestResponse getById(Long id);
}