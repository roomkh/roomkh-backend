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
public class SellerRequestResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String position;
    private String businessName;
    private String reason;
    private boolean termsAccepted;
    private SellerRequestStatus status;
    private String adminNote;
    private OffsetDateTime contactedAt;
    private Long contactedBy;
    private OffsetDateTime reviewedAt;
    private Long reviewedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}