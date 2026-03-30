package com.founderlink.analyticsservice.repository;

import com.founderlink.analyticsservice.entity.StartupTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StartupTeamMemberRepository extends JpaRepository<StartupTeamMember, Long> {

  long countByStartupId(Long startupId);

  boolean existsByStartupIdAndUserId(Long startupId, Long userId);
}
