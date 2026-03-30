package com.founderlink.teamservice.service;

import com.founderlink.teamservice.config.RabbitMqConfig;
import com.founderlink.teamservice.dto.TeamInviteRequest;
import com.founderlink.teamservice.dto.TeamJoinRequest;
import com.founderlink.teamservice.dto.TeamMemberResponse;
import com.founderlink.teamservice.entity.TeamMembership;
import com.founderlink.teamservice.repository.TeamMembershipRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Team membership lifecycle: founders record invitations; invitees accept (join) to become ACTIVE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

  private final TeamMembershipRepository repository;
  private final RabbitTemplate rabbitTemplate;

  @Transactional
  public TeamMemberResponse invite(Long founderUserId, TeamInviteRequest request) {
    repository
        .findByStartupIdAndUserId(request.startupId(), request.invitedUserId())
        .ifPresent(
            m -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "User already has a team row for this startup");
            });

    TeamMembership row =
        TeamMembership.builder()
            .startupId(request.startupId())
            .userId(request.invitedUserId())
            .teamRole(request.role().trim().toUpperCase())
            .status(TeamMembership.Status.PENDING)
            .invitedByUserId(founderUserId)
            .createdAt(LocalDateTime.now())
            .build();
    try {
      TeamMembership saved = repository.save(row);
      publishTeamInviteSent(founderUserId, request);
      return TeamMemberResponse.from(saved);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Duplicate team membership prevented by database constraint");
    }
  }

  private void publishTeamInviteSent(Long founderUserId, TeamInviteRequest request) {
    Map<String, Object> event = new LinkedHashMap<>();
    event.put("startupId", request.startupId());
    event.put("invitedUserId", request.invitedUserId());
    event.put("role", request.role());
    event.put("founderId", founderUserId);
    try {
      rabbitTemplate.convertAndSend(RabbitMqConfig.FOUNDERLINK_EXCHANGE, "team.invite.sent", event);
      log.info(
          "Published team.invite.sent startupId={} invitedUserId={}",
          request.startupId(),
          request.invitedUserId());
    } catch (Exception ex) {
      log.error("Failed to publish team.invite.sent: {}", ex.toString());
      throw ex;
    }
  }

  @Transactional
  public TeamMemberResponse join(Long currentUserId, TeamJoinRequest request) {
    TeamMembership row =
        repository
            .findByStartupIdAndUserId(request.startupId(), currentUserId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No invitation found for this startup and user; ask a founder to invite you first"));

    if (row.getStatus() == TeamMembership.Status.ACTIVE) {
      return TeamMemberResponse.from(row);
    }
    if (row.getStatus() != TeamMembership.Status.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Membership is not in PENDING state");
    }
    row.setStatus(TeamMembership.Status.ACTIVE);
    row.setJoinedAt(LocalDateTime.now());
    return TeamMemberResponse.from(repository.save(row));
  }

  public List<TeamMemberResponse> listForStartup(Long startupId) {
    return repository.findByStartupIdOrderByCreatedAtAsc(startupId).stream()
        .map(TeamMemberResponse::from)
        .toList();
  }
}
