package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.SellerPropertyListItemResponse;
import com.roomkh.backend.dto.property.SellerPropertyStatusCountsResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor
public class SellerPropertyListResult {
    private final Page<SellerPropertyListItemResponse> page;
    private final SellerPropertyStatusCountsResponse statusCounts;
}