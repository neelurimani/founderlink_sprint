package com.founderlink.startupservice.repository;

import com.founderlink.startupservice.entity.Startup;
import com.founderlink.startupservice.entity.StartupStage;
import com.founderlink.startupservice.entity.StartupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StartupRepository extends JpaRepository<Startup, Long> {

  boolean existsByFounderIdAndNameIgnoreCase(Long founderId, String name);

  Page<Startup> findByStatus(StartupStatus status, Pageable pageable);

  Page<Startup> findByIndustryIgnoreCaseAndStatus(
      String industry, StartupStatus status, Pageable pageable);

  Page<Startup> findByIndustryIgnoreCase(String industry, Pageable pageable);

  Page<Startup> findByStage(StartupStage stage, Pageable pageable);
}
