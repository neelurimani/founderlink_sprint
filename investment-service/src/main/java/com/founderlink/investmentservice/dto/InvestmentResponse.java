package com.founderlink.investmentservice.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentResponse {
  private Long id;
  private Long investorId;
  private Long startupId;
  private Double amount;
  private String status;
  private LocalDateTime createdAt;
}
