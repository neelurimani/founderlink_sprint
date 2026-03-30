package com.founderlink.analyticsservice.controller;

import com.founderlink.analyticsservice.dto.FundingRecordResponse;
import com.founderlink.analyticsservice.dto.FundingTrendResponse;
import com.founderlink.analyticsservice.dto.InvestorPortfolioResponse;
import com.founderlink.analyticsservice.dto.StartupMetricsResponse;
import com.founderlink.analyticsservice.service.AnalyticsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  @GetMapping("/funding-trends")
  @PreAuthorize("isAuthenticated()")
  public FundingTrendResponse fundingTrends() {
    return analyticsService.fundingTrends();
  }

  @GetMapping("/reports")
  @PreAuthorize("isAuthenticated()")
  public List<FundingRecordResponse> reports() {
    return analyticsService.reports();
  }

  @GetMapping("/startups/{startupId}/metrics")
  @PreAuthorize("isAuthenticated()")
  public StartupMetricsResponse startupMetrics(@PathVariable Long startupId) {
    return analyticsService.startupMetrics(startupId);
  }

  @GetMapping("/investors/{investorId}/portfolio")
  @PreAuthorize("isAuthenticated()")
  public InvestorPortfolioResponse investorPortfolio(@PathVariable Long investorId) {
    return analyticsService.investorPortfolio(investorId);
  }
}
