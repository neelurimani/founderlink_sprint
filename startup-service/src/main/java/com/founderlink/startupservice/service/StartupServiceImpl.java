package com.founderlink.startupservice.service;

import com.founderlink.startupservice.dto.StartupDto;
import com.founderlink.startupservice.dto.StartupResponse;
import com.founderlink.startupservice.entity.Startup;
import com.founderlink.startupservice.entity.StartupStatus;
import com.founderlink.startupservice.exception.ConflictException;
import com.founderlink.startupservice.exception.ForbiddenUpdateException;
import com.founderlink.startupservice.exception.NotFoundException;
import com.founderlink.startupservice.exception.UnauthorizedException;
import com.founderlink.startupservice.repository.StartupRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class StartupServiceImpl implements StartupService {

  private final StartupRepository startupRepository;
  private final RabbitTemplate rabbitTemplate;

  @Override
  @Transactional
  @CacheEvict(cacheNames = "startups", allEntries = true)
  public StartupResponse createStartup(StartupDto dto) {
    ensureCallerIsFounder();
    Long callerUserId = requireCallerUserId();

    String name = dto.getName() == null ? "" : dto.getName().trim();
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("Name must not be blank");
    }
    if (dto.getFundingGoal() == null || dto.getFundingGoal() <= 0) {
      throw new IllegalArgumentException("Funding goal must be greater than 0");
    }

    if (startupRepository.existsByFounderIdAndNameIgnoreCase(callerUserId, name)) {
      throw new ConflictException("Startup already exists for this founder");
    }

    Startup startup = mapToEntity(dto, callerUserId, name);
    Startup saved;
    try {
      saved = startupRepository.save(startup);
    } catch (DataIntegrityViolationException ex) {
      log.warn(
          "Duplicate startup blocked at DB: founderId={} name={} msg={}",
          callerUserId,
          name,
          ex.getMostSpecificCause().getMessage());
      throw new ConflictException("Startup already exists for this founder");
    }

    Map<String, Object> event = new LinkedHashMap<>();
    event.put("startupId", saved.getId());
    event.put("founderId", saved.getFounderId());
    event.put("industry", saved.getIndustry());
    event.put("fundingGoal", saved.getFundingGoal());
    rabbitTemplate.convertAndSend("founderlink.exchange", "startup.created", event);
    log.info(
        "Startup created id={} founderId={} status={}",
        saved.getId(),
        saved.getFounderId(),
        saved.getStatus());

    return mapToResponse(saved);
  }

  @Override
  public StartupResponse getStartupById(Long id) {
    Startup startup = requireStartup(id);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (isAnonymous(authentication)) {
      if (startup.getStatus() != StartupStatus.APPROVED) {
        throw new ForbiddenUpdateException("Startup is not approved");
      }
      return mapToResponse(startup);
    }

    Long callerUserId = requireCallerUserId();
    boolean isAdmin = hasRole("ADMIN");
    if (isAdmin) {
      return mapToResponse(startup);
    }

    boolean isFounderOwner = java.util.Objects.equals(startup.getFounderId(), callerUserId);
    if (isFounderOwner) {
      return mapToResponse(startup);
    }

    if (startup.getStatus() != StartupStatus.APPROVED) {
      throw new ForbiddenUpdateException("Startup is not approved");
    }
    return mapToResponse(startup);
  }

  private static boolean isAnonymous(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return true;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof String s && "anonymousUser".equals(s)) {
      return true;
    }
    String name = authentication.getName();
    return name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name);
  }

  @Override
  public Page<StartupResponse> getAllStartups(int page, int size) {
    var pageable = PageRequest.of(page, size);
    if (hasRole("ADMIN")) {
      return startupRepository.findAll(pageable).map(this::mapToResponse);
    }
    return startupRepository
        .findByStatus(StartupStatus.APPROVED, pageable)
        .map(this::mapToResponse);
  }

  @Override
  public Page<StartupResponse> getStartupsByIndustry(String industry, int page, int size) {
    String trimmed = industry == null ? "" : industry.trim();
    if (!StringUtils.hasText(trimmed)) {
      throw new IllegalArgumentException("Industry must not be blank");
    }
    var pageable = PageRequest.of(page, size);
    if (hasRole("ADMIN")) {
      return startupRepository.findByIndustryIgnoreCase(trimmed, pageable).map(this::mapToResponse);
    }
    return startupRepository
        .findByIndustryIgnoreCaseAndStatus(trimmed, StartupStatus.APPROVED, pageable)
        .map(this::mapToResponse);
  }

  @Override
  @CacheEvict(cacheNames = "startups", key = "#id")
  public StartupResponse updateStartup(Long id, StartupDto dto) {
    Startup startup = requireStartup(id);
    Long callerUserId = requireCallerUserId();

    boolean isAdmin = hasRole("ADMIN");
    if (isAdmin) {
      if (dto.getStatus() == null) {
        throw new ForbiddenUpdateException("ADMIN updates require status (APPROVED or REJECTED)");
      }
      StartupStatus previous = startup.getStatus();
      startup.setStatus(dto.getStatus());
      Startup saved = startupRepository.save(startup);
      if (dto.getStatus() == StartupStatus.APPROVED && previous != StartupStatus.APPROVED) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("startupId", saved.getId());
        event.put("founderId", saved.getFounderId());
        event.put("industry", saved.getIndustry());
        event.put("fundingGoal", saved.getFundingGoal());
        rabbitTemplate.convertAndSend("founderlink.exchange", "startup.approved", event);
        log.info("Startup approved id={}", saved.getId());
      }
      return mapToResponse(saved);
    }

    if (!java.util.Objects.equals(startup.getFounderId(), callerUserId)) {
      throw new ForbiddenUpdateException("You can only update your own startup");
    }
    if (dto.getStatus() != null) {
      throw new ForbiddenUpdateException("Only ADMIN can change startup status");
    }

    if (dto.getName() != null) {
      String newName = dto.getName().trim();
      if (!StringUtils.hasText(newName)) {
        throw new IllegalArgumentException("Name must not be blank");
      }
      if (!newName.equalsIgnoreCase(startup.getName())
          && startupRepository.existsByFounderIdAndNameIgnoreCase(callerUserId, newName)) {
        throw new ConflictException("Startup already exists for this founder");
      }
      startup.setName(newName);
    }
    if (dto.getDescription() != null) {
      startup.setDescription(dto.getDescription());
    }
    if (dto.getIndustry() != null) {
      startup.setIndustry(dto.getIndustry());
    }
    if (dto.getProblemStatement() != null) {
      startup.setProblemStatement(dto.getProblemStatement());
    }
    if (dto.getSolution() != null) {
      startup.setSolution(dto.getSolution());
    }
    if (dto.getFundingGoal() != null) {
      if (dto.getFundingGoal() <= 0) {
        throw new IllegalArgumentException("Funding goal must be greater than 0");
      }
      startup.setFundingGoal(dto.getFundingGoal());
    }
    if (dto.getStage() != null) {
      startup.setStage(dto.getStage());
    }

    try {
      return mapToResponse(startupRepository.save(startup));
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("Startup already exists for this founder");
    }
  }

  @Override
  @CacheEvict(cacheNames = "startups", key = "#id")
  public void deleteStartup(Long id) {
    Startup startup = requireStartup(id);
    Long callerUserId = requireCallerUserId();
    if (!startup.getFounderId().equals(callerUserId)) {
      throw new ForbiddenUpdateException("You can only delete your own startup");
    }
    startupRepository.deleteById(id);
  }

  private Startup mapToEntity(StartupDto dto, Long founderId, String trimmedName) {
    return Startup.builder()
        .name(trimmedName)
        .description(dto.getDescription())
        .industry(dto.getIndustry())
        .problemStatement(dto.getProblemStatement())
        .solution(dto.getSolution())
        .fundingGoal(dto.getFundingGoal())
        .stage(dto.getStage())
        .founderId(founderId)
        .status(StartupStatus.PENDING)
        .build();
  }

  private Startup requireStartup(Long id) {
    return startupRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Startup not found"));
  }

  private StartupResponse mapToResponse(Startup startup) {
    return StartupResponse.builder()
        .id(startup.getId())
        .name(startup.getName())
        .description(startup.getDescription())
        .industry(startup.getIndustry())
        .problemStatement(startup.getProblemStatement())
        .solution(startup.getSolution())
        .fundingGoal(startup.getFundingGoal())
        .stage(startup.getStage())
        .founderId(startup.getFounderId())
        .status(startup.getStatus())
        .createdAt(startup.getCreatedAt())
        .build();
  }

  private Long requireCallerUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || authentication.getName().isBlank()) {
      throw new UnauthorizedException("Missing authentication");
    }
    try {
      return Long.valueOf(authentication.getName());
    } catch (NumberFormatException ex) {
      throw new UnauthorizedException("Invalid authenticated user id");
    }
  }

  private boolean hasRole(String role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getAuthorities() == null) {
      return false;
    }
    String expected = "ROLE_" + role;
    return authentication.getAuthorities().stream()
        .anyMatch(a -> expected.equals(a.getAuthority()));
  }

  private void ensureCallerIsFounder() {
    if (!hasRole("FOUNDER")) {
      throw new ForbiddenUpdateException("Only FOUNDERS can create startups");
    }
  }
}
