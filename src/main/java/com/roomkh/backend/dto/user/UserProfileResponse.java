package com.roomkh.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    
    @JsonProperty("full_name")
    private String fullName;
    
    private String email;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    private String role;
    
    @JsonProperty("seller_status")
    private String sellerStatus;
    
    @JsonProperty("auth_provider")
    private String authProvider;
    
    @JsonProperty("joined_at")
    private LocalDateTime joinedAt;
}