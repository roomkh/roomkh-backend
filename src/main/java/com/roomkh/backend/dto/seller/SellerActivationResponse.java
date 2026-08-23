package com.roomkh.backend.dto.seller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerActivationResponse {
    private Long userId;
    private String phoneNumber;
    private String role;
}