package com.founderlink.userservice.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FounderProfileResponse extends UserProfileResponse {
    private String startupName;
    private String industry;
    private Double fundingGoal;
}
