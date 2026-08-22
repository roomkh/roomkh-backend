package com.roomkh.backend.dto.seller;

import com.roomkh.backend.entity.SellerRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRequestSummaryResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String businessName;
    private SellerRequestStatus status;
    private OffsetDateTime createdAt;
}