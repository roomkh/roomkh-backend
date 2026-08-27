package com.roomkh.backend.dto.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.roomkh.backend.entity.SellerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardResponse {

    @JsonProperty("seller_status")
    private SellerStatus sellerStatus;

    @JsonProperty("total_properties")
    private long totalProperties;

    @JsonProperty("active_count")
    private long activeCount;

    @JsonProperty("pending_count")
    private long pendingCount;

    @JsonProperty("draft_count")
    private long draftCount;

    @JsonProperty("sold_rented_count")
    private long soldRentedCount;

    @JsonProperty("total_views")
    private long totalViews;

    @JsonProperty("total_inquiries")
    private long totalInquiries;
}