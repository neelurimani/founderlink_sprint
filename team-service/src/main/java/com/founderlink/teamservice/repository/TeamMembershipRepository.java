package com.founderlink.teamservice.repository;

import com.founderlink.teamservice.entity.TeamMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {

  List<TeamMembership> findByStartupIdOrderByCreatedAtAsc(Long startupId);

  Optional<TeamMembership> findByStartupIdAndUserId(Long startupId, Long userId);
}
