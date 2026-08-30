package com.roomkh.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPropertyStatsResponse {

    @JsonProperty("total_listings")
    private Long totalListings;

    @JsonProperty("total_trend")
    private Double totalTrend;

    @JsonProperty("active_listings")
    private Long activeListings;

    @JsonProperty("active_trend")
    private Double activeTrend;

    @JsonProperty("pending_listings")
    private Long pendingListings;

    @JsonProperty("pending_trend")
    private Double pendingTrend;

    @JsonProperty("inactive_listings")
    private Long inactiveListings;

    @JsonProperty("inactive_trend")
    private Double inactiveTrend;
}