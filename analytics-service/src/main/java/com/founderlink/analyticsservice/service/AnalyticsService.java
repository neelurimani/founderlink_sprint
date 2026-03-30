package com.founderlink.analyticsservice.service;

import com.founderlink.analyticsservice.dto.FundingRecordResponse;
import com.founderlink.analyticsservice.dto.FundingTrendResponse;
import com.founderlink.analyticsservice.dto.InvestorPortfolioResponse;
import com.founderlink.analyticsservice.dto.StartupMetricsResponse;
import com.founderlink.analyticsservice.entity.FundingRecord;
import com.founderlink.analyticsservice.entity.StartupSnapshot;
import com.founderlink.analyticsservice.repository.FundingRecordRepository;
import com.founderlink.analyticsservice.repository.StartupSnapshotRepository;
import com.founderlink.analyticsservice.repository.StartupTeamMemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

  private final FundingRecordRepository fundingRecordRepository;
  private final StartupSnapshotRepository snapshotRepository;
  private final StartupTeamMemberRepository teamMemberRepository;

  public FundingTrendResponse fundingTrends() {
    return FundingTrendResponse.builder()
        .recordCount(fundingRecordRepository.count())
        .totalFunding(safe(fundingRecordRepository.totalFunding()))
        .averageFunding(safe(fundingRecordRepository.averageFunding()))
        .build();
  }

  public List<FundingRecordResponse> reports() {
    return fundingRecordRepository.findTop200ByOrderByCreatedDateDescIdDesc().stream()
        .map(this::toResponse)
        .toList();
  }

  public StartupMetricsResponse startupMetrics(Long startupId) {
    Double invested = fundingRecordRepository.sumAmountByStartupId(startupId);
    double totalInvested = invested != null ? invested : 0.0;
    StartupSnapshot snap = snapshotRepository.findById(startupId).orElse(null);
    Double goal = snap != null ? snap.getFundingGoal() : null;
    Double progress = null;
    if (goal != null && goal > 0) {
      progress = Math.min(100.0, (totalInvested / goal) * 100.0);
    }
    long teamRows = teamMemberRepository.countByStartupId(startupId);
    long teamSize = teamRows + (snap != null && snap.getFounderId() != null ? 1L : 0L);
    long invCount = fundingRecordRepository.countByStartupId(startupId);
    return StartupMetricsResponse.builder()
        .startupId(startupId)
        .totalInvested(totalInvested)
        .fundingGoal(goal)
        .fundingProgressPercent(progress)
        .teamSize(teamSize)
        .investmentCount(invCount)
        .build();
  }

  public InvestorPortfolioResponse investorPortfolio(Long investorId) {
    Double s = fundingRecordRepository.sumAmountByInvestorId(investorId);
    double total = s != null ? s : 0.0;
    long count = fundingRecordRepository.countByInvestorId(investorId);
    return InvestorPortfolioResponse.builder()
        .investorId(investorId)
        .totalInvested(total)
        .investmentCount(count)
        .build();
  }

  private FundingRecordResponse toResponse(FundingRecord record) {
    return new FundingRecordResponse(
        record.getId(),
        record.getInvestmentId(),
        record.getStartupId(),
        record.getInvestorId(),
        record.getAmount(),
        record.getStatus(),
        record.getCreatedDate());
  }

  private static double safe(Double v) {
    return v != null ? v : 0.0;
  }
}
