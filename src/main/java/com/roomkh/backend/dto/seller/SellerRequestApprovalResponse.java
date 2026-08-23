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
public class SellerRequestApprovalResponse {
    private Long sellerRequestId;
    private SellerRequestStatus status;
    private Long userId;
    private String role;
    private OffsetDateTime otpExpiresAt;
}