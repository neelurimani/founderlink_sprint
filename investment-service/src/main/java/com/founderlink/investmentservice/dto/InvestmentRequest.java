package com.founderlink.investmentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InvestmentRequest {
  @NotNull private Long investorId;
  @NotNull private Long startupId;
  @NotNull @Positive private Double amount;
}
