package com.founderlink.teamservice.dto;

import com.founderlink.teamservice.entity.TeamMembership;

public record TeamMemberResponse(
    Long id, Long startupId, Long userId, String role, String status, Long invitedByUserId) {
  public static TeamMemberResponse from(TeamMembership m) {
    return new TeamMemberResponse(
        m.getId(),
        m.getStartupId(),
        m.getUserId(),
        m.getTeamRole(),
        m.getStatus().name(),
        m.getInvitedByUserId());
  }
}
