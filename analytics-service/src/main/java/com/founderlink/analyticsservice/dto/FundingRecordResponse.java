package com.founderlink.analyticsservice.dto;

import java.time.LocalDate;

public record FundingRecordResponse(
    Long id,
    Long investmentId,
    Long startupId,
    Long investorId,
    Double amount,
    String status,
    LocalDate createdDate) {}
