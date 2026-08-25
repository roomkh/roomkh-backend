package com.roomkh.backend.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPropertyStatusCountsResponse {
    private long all;
    private long active;
    private long pending;
    private long draft;
    private long soldRented;
}