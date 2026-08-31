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
public class AdminUserStatsResponse {

    @JsonProperty("total_users")
    private long totalUsers;

    @JsonProperty("total_owners")
    private long totalOwners;

    @JsonProperty("total_agents")
    private long totalAgents;

    @JsonProperty("active_this_month")
    private long activeThisMonth;

    @JsonProperty("owners_pending_approval")
    private long ownersPendingApproval;
}