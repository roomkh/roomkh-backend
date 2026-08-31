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
public class AdminOwnerStatsResponse {

    @JsonProperty("total_owners")
    private long totalOwners;

    @JsonProperty("active_owners")
    private long activeOwners;

    @JsonProperty("pending_owners")
    private long pendingOwners;

    @JsonProperty("inactive_owners")
    private long inactiveOwners;
}