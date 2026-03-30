package com.founderlink.investmentservice.repository;

import com.founderlink.investmentservice.entity.Investment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {

  List<Investment> findByInvestorId(Long investorId);

  List<Investment> findByStartupId(Long startupId);

  boolean existsByStartupIdAndInvestorId(Long startupId, Long investorId);
}
