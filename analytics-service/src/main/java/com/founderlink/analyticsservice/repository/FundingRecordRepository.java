package com.founderlink.analyticsservice.repository;

import com.founderlink.analyticsservice.entity.FundingRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingRecordRepository extends JpaRepository<FundingRecord, Long> {

  boolean existsByInvestmentId(Long investmentId);

  Optional<FundingRecord> findByInvestmentId(Long investmentId);

  Optional<FundingRecord> findByStartupIdAndInvestorId(Long startupId, Long investorId);

  @Query("select coalesce(sum(f.amount), 0) from FundingRecord f")
  Double totalFunding();

  @Query("select coalesce(avg(f.amount), 0) from FundingRecord f")
  Double averageFunding();

  @Query("select coalesce(sum(f.amount), 0) from FundingRecord f where f.startupId = :startupId")
  Double sumAmountByStartupId(@Param("startupId") Long startupId);

  @Query("select coalesce(sum(f.amount), 0) from FundingRecord f where f.investorId = :investorId")
  Double sumAmountByInvestorId(@Param("investorId") Long investorId);

  long countByStartupId(Long startupId);

  long countByInvestorId(Long investorId);

  List<FundingRecord> findTop200ByOrderByCreatedDateDescIdDesc();
}
