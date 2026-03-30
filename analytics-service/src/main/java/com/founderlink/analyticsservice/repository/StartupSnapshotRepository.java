package com.founderlink.analyticsservice.repository;

import com.founderlink.analyticsservice.entity.StartupSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StartupSnapshotRepository extends JpaRepository<StartupSnapshot, Long> {}
