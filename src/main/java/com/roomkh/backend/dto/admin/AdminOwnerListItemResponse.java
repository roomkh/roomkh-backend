package com.roomkh.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOwnerListItemResponse {
    private Long id;
    private String ownerName;
    private String email;
    private String phoneNumber;
    private String plan;
    private long propertiesCount;
    private LocalDateTime joinDate;
    private String status;
}