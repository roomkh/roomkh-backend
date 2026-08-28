package com.roomkh.backend.dto.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerContactResponse {

    private Long id;
    private String name;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    private String email;
}