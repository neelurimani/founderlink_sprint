package com.founderlink.userservice.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CoFounderProfileResponse extends UserProfileResponse {
    private String expertise;
    private String coFounderExperience;
}
