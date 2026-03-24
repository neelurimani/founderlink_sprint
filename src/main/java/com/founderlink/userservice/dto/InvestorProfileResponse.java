package com.founderlink.userservice.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InvestorProfileResponse extends UserProfileResponse {
    private Double investmentBudget;
    private String preferredIndustries;
}
