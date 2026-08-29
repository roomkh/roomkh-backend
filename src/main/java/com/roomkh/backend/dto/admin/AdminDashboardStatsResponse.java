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
public class AdminDashboardStatsResponse {

    @JsonProperty("total_users")
    private Long totalUsers;

    @JsonProperty("total_owners")
    private Long totalOwners;

    @JsonProperty("total_listings")
    private Long totalListings;

    @JsonProperty("pending_listings")
    private Long pendingListings;

    @JsonProperty("monthly_revenue")
    private Double monthlyRevenue;
}