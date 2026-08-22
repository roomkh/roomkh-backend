package com.roomkh.backend.mapper;

import com.roomkh.backend.dto.seller.SellerRequestResponse;
import com.roomkh.backend.dto.seller.SellerRequestSummaryResponse;
import com.roomkh.backend.entity.SellerRequest;
import org.springframework.stereotype.Component;

@Component
public class SellerRequestMapper {

    public SellerRequestResponse toResponse(SellerRequest request) {
        return SellerRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUser() != null ? request.getUser().getId() : null)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .position(request.getPosition())
                .businessName(request.getBusinessName())
                .reason(request.getReason())
                .termsAccepted(request.isTermsAccepted())
                .status(request.getStatus())
                .adminNote(request.getAdminNote())
                .contactedAt(request.getContactedAt())
                .contactedBy(request.getContactedBy() != null ? request.getContactedBy().getId() : null)
                .reviewedAt(request.getReviewedAt())
                .reviewedBy(request.getReviewedBy() != null ? request.getReviewedBy().getId() : null)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    public SellerRequestSummaryResponse toSummaryResponse(SellerRequest request) {
        return SellerRequestSummaryResponse.builder()
                .id(request.getId())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .businessName(request.getBusinessName())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}