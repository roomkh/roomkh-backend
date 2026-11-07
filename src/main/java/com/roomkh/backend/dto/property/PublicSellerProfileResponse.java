package com.roomkh.backend.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public profile of a seller: the same contact details the property detail
 * page already shows, plus every ACTIVE listing they own (newest first).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSellerProfileResponse {

    private Long id;
    private String name;
    private String avatarUrl;
    private String phoneNumber;
    private LocalDateTime joinedAt;
    private int listingCount;
    private List<PublicPropertyListItemResponse> listings;
}
