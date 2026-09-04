package com.roomkh.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
    public class PlatformGrowthDto {
    private LocalDate date;
    private Integer newUsers;
    private Integer newListings;
}