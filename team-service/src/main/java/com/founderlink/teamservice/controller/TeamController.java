package com.founderlink.teamservice.controller;

import com.founderlink.teamservice.dto.TeamInviteRequest;
import com.founderlink.teamservice.dto.TeamJoinRequest;
import com.founderlink.teamservice.dto.TeamMemberResponse;
import com.founderlink.teamservice.service.TeamService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** APIs per FounderLink design §7.5: invite co-founder, join team, list members for a startup. */
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

  private final TeamService teamService;

  @PostMapping("/invite")
  @PreAuthorize("hasAnyRole('FOUNDER','ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public TeamMemberResponse invite(
      Authentication authentication, @Valid @RequestBody TeamInviteRequest body) {
    return teamService.invite(requireUserId(authentication), body);
  }

  @PostMapping("/join")
  public TeamMemberResponse join(
      Authentication authentication, @Valid @RequestBody TeamJoinRequest body) {
    return teamService.join(requireUserId(authentication), body);
  }

  @GetMapping("/startup/{startupId}")
  public ResponseEntity<List<TeamMemberResponse>> list(@PathVariable Long startupId) {
    return ResponseEntity.ok(teamService.listForStartup(startupId));
  }

  private Long requireUserId(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    try {
      return Long.valueOf(authentication.getName());
    } catch (NumberFormatException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user principal");
    }
  }
}
