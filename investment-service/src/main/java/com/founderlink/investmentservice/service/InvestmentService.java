package com.founderlink.investmentservice.service;

import com.founderlink.investmentservice.client.StartupServiceClient;
import com.founderlink.investmentservice.client.UserServiceClient;
import com.founderlink.investmentservice.client.view.StartupView;
import com.founderlink.investmentservice.client.view.UserProfileView;
import com.founderlink.investmentservice.dto.InvestmentRequest;
import com.founderlink.investmentservice.dto.InvestmentResponse;
import com.founderlink.investmentservice.entity.Investment;
import com.founderlink.investmentservice.repository.InvestmentRepository;
import feign.FeignException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentService {

  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_APPROVED = "APPROVED";

  private final InvestmentRepository repository;
  private final RabbitTemplate rabbitTemplate;
  private final StartupServiceClient startupServiceClient;
  private final UserServiceClient userServiceClient;
  private final Environment environment;

  /**
   * Creates a single investment per (startup, investor) pair — enforced in DB and here under a
   * transaction.
   */
  @Transactional(rollbackFor = Exception.class)
  public InvestmentResponse createInvestment(InvestmentRequest request) {
    Long callerUserId = requireCallerUserId();

    if (!hasRole("ADMIN") && !hasRole("INVESTOR")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only investors can invest");
    }

    if (!hasRole("ADMIN") && !Objects.equals(request.getInvestorId(), callerUserId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "investorId in the body must equal your authenticated user id (from JWT / X-User-Id), unless you have role ADMIN");
    }

    if (repository.existsByStartupIdAndInvestorId(
        request.getStartupId(), request.getInvestorId())) {
      log.warn(
          "Duplicate investment blocked (application check): startupId={} investorId={}",
          request.getStartupId(),
          request.getInvestorId());
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate investment not allowed");
    }

    StartupView startup = requireStartup(request.getStartupId());
    if (startup.founderId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Startup response missing founderId for id " + request.getStartupId());
    }
    if (startup.status() == null || !"APPROVED".equalsIgnoreCase(startup.status())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Startup is not approved for investment");
    }
    ensureInvestorExists(request.getInvestorId());

    Investment investment =
        Investment.builder()
            .investorId(request.getInvestorId())
            .startupId(request.getStartupId())
            .amount(request.getAmount())
            .status(STATUS_PENDING)
            .createdAt(LocalDateTime.now())
            .build();

    Investment saved;
    try {
      saved = repository.save(investment);
    } catch (DataIntegrityViolationException ex) {
      log.warn(
          "Duplicate investment blocked (DB constraint): startupId={} investorId={} msg={}",
          request.getStartupId(),
          request.getInvestorId(),
          ex.getMostSpecificCause().getMessage());
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate investment not allowed");
    }

    log.info(
        "Investment created id={} startupId={} investorId={} amount={}",
        saved.getId(),
        saved.getStartupId(),
        saved.getInvestorId(),
        saved.getAmount());

    Map<String, Object> createdEvent = new LinkedHashMap<>();
    createdEvent.put("investmentId", saved.getId());
    createdEvent.put("startupId", saved.getStartupId());
    createdEvent.put("founderId", startup.founderId());
    createdEvent.put("investorId", saved.getInvestorId());
    createdEvent.put("amount", saved.getAmount());
    try {
      rabbitTemplate.convertAndSend("founderlink.exchange", "investment.created", createdEvent);
      log.info(
          "Published investment.created routingKey=investment.created startupId={}",
          saved.getStartupId());
    } catch (Exception ex) {
      log.error("Failed to publish investment.created event: {}", ex.toString());
      throw ex;
    }

    return mapToResponse(saved);
  }

  private StartupView requireStartup(Long startupId) {
    log.info(
        "Verifying startup via Feign: startupId={} target={}",
        startupId,
        startupFeignTargetDescription());
    try {
      StartupView view = startupServiceClient.getStartupById(startupId);
      if (view == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup not found");
      }
      return view;
    } catch (FeignException.NotFound ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup not found");
    } catch (FeignException ex) {
      int code = ex.status();
      log.warn(
          "startup-service Feign failure: startupId={} feignHttpStatus={} target={} exceptionClass={} detail={}",
          startupId,
          code,
          startupFeignTargetDescription(),
          ex.getClass().getSimpleName(),
          ex.getMessage());
      if (code == 403) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Startup is not approved for investment");
      }
      if (code <= 0 || code == 503) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Startup service is currently unavailable. Please try again later.");
      }
      String body = safeFeignBody(ex);
      String hint =
          code == 401
              ? " (startup-service rejected the call—check JWT/headers; GET /startups/{id} must be reachable)"
              : "";
      String msg =
          "Unable to verify startup (HTTP "
              + code
              + " from startup-service)"
              + hint
              + (body.isEmpty() ? "" : ": " + body);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg);
    } catch (IllegalStateException ex) {
      // Spring Cloud LoadBalancer when no instance is registered for startup-service
      if (isLoadBalancerNoInstance(ex)) {
        log.warn(
            "startup-service: no load-balanced instance (Eureka empty or stale): startupId={} target={} msg={}",
            startupId,
            startupFeignTargetDescription(),
            ex.getMessage());
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Startup service is currently unavailable. Please try again later.");
      }
      throw ex;
    }
  }

  private static boolean isLoadBalancerNoInstance(IllegalStateException ex) {
    String m = ex.getMessage();
    return m != null
        && (m.contains("Load balancer does not contain an instance")
            || m.contains("No instances available")
            || m.contains("available server for client"));
  }

  private String startupFeignTargetDescription() {
    String url =
        environment.getProperty("spring.cloud.openfeign.client.config.startup-service.url");
    if (StringUtils.hasText(url)) {
      return "static-url=" + url;
    }
    return "serviceId=startup-service (Eureka/LoadBalancer)";
  }

  private void ensureInvestorExists(Long investorId) {
    if (investorId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "investorId must not be null");
    }
    log.info(
        "Verifying investor via Feign: investorId={} target={}",
        investorId,
        userFeignTargetDescription());
    try {
      UserProfileView profile = userServiceClient.getUserById(investorId);
      if (profile == null || profile.id() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Investor not found");
      }
      log.info("user-service profile received: investorId={} role={}", investorId, profile.role());
      String role = profile.role();
      if (role == null || role.isBlank() || !"INVESTOR".equalsIgnoreCase(role.trim())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an investor");
      }
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (FeignException.NotFound ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Investor not found");
    } catch (FeignException ex) {
      int code = ex.status();
      log.warn(
          "user-service Feign failure: investorId={} feignHttpStatus={} target={} exceptionClass={} detail={}",
          investorId,
          code,
          userFeignTargetDescription(),
          ex.getClass().getSimpleName(),
          ex.getMessage());
      if (code <= 0 || code == 503) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "User service is currently unavailable");
      }
      if (code == 401) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Unable to verify investor: user-service rejected authentication (forward Authorization from gateway)");
      }
      if (code == 403) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Not allowed to load investor profile from user-service");
      }
      String body = safeFeignBody(ex);
      String msg =
          "Unable to verify investor (HTTP "
              + code
              + " from user-service)"
              + (body.isEmpty() ? "" : ": " + body);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg);
    } catch (IllegalStateException ex) {
      if (isLoadBalancerNoInstance(ex)) {
        log.warn(
            "user-service: no load-balanced instance: investorId={} target={} msg={}",
            investorId,
            userFeignTargetDescription(),
            ex.getMessage());
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "User service is currently unavailable");
      }
      throw ex;
    }
  }

  private String userFeignTargetDescription() {
    String url = environment.getProperty("spring.cloud.openfeign.client.config.user-service.url");
    if (StringUtils.hasText(url)) {
      return "static-url=" + url;
    }
    return "serviceId=user-service (Eureka/LoadBalancer)";
  }

  public List<InvestmentResponse> getByInvestor(Long investorId) {
    Long callerUserId = requireCallerUserId();
    if (!hasRole("ADMIN") && !investorId.equals(callerUserId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You can only view your own investments");
    }
    return repository.findByInvestorId(investorId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public List<InvestmentResponse> getByStartup(Long startupId) {
    Long callerUserId = requireCallerUserId();

    if (hasRole("ADMIN")) {
      return repository.findByStartupId(startupId).stream()
          .map(this::mapToResponse)
          .collect(Collectors.toList());
    }

    if (hasRole("INVESTOR")) {
      // Investors can only track their own investments.
      return repository.findByStartupId(startupId).stream()
          .filter(inv -> callerUserId.equals(inv.getInvestorId()))
          .map(this::mapToResponse)
          .collect(Collectors.toList());
    }

    if (hasRole("FOUNDER")) {
      // Founders can track investments for their own startup.
      Object startup = startupServiceClient.getStartupById(startupId);
      Long founderId = extractFounderId(startup);
      if (founderId == null || !callerUserId.equals(founderId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You can only view investments for your own startup");
      }

      return repository.findByStartupId(startupId).stream()
          .map(this::mapToResponse)
          .collect(Collectors.toList());
    }

    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "You are not allowed to view these investments");
  }

  public InvestmentResponse approveInvestment(Long id) {
    Investment investment =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Investment not found: " + id));

    if (!hasRole("ADMIN")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can approve investments");
    }

    investment.setStatus(STATUS_APPROVED);
    Investment saved = repository.save(investment);

    Map<String, Object> approvedEvent = new LinkedHashMap<>();
    approvedEvent.put("investmentId", saved.getId());
    approvedEvent.put("startupId", saved.getStartupId());
    approvedEvent.put("investorId", saved.getInvestorId());
    approvedEvent.put("amount", saved.getAmount());
    rabbitTemplate.convertAndSend("founderlink.exchange", "investment.approved", approvedEvent);

    return mapToResponse(saved);
  }

  private InvestmentResponse mapToResponse(Investment investment) {
    return InvestmentResponse.builder()
        .id(investment.getId())
        .investorId(investment.getInvestorId())
        .startupId(investment.getStartupId())
        .amount(investment.getAmount())
        .status(investment.getStatus())
        .createdAt(investment.getCreatedAt())
        .build();
  }

  private Long requireCallerUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || authentication.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication");
    }
    try {
      return Long.valueOf(authentication.getName());
    } catch (NumberFormatException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user id");
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

  private static String safeFeignBody(FeignException ex) {
    try {
      String c = ex.contentUTF8();
      return c == null || c.isBlank() ? "" : c.trim();
    } catch (Exception e) {
      return "";
    }
  }

  private Long extractFounderId(Object startupDto) {
    if (startupDto == null) {
      return null;
    }
    try {
      var method = startupDto.getClass().getMethod("getFounderId");
      Object val = method.invoke(startupDto);
      if (val instanceof Long l) {
        return l;
      }
      if (val instanceof Integer i) {
        return i.longValue();
      }
      return null;
    } catch (Exception ignored) {
    }
    try {
      var method = startupDto.getClass().getMethod("founderId");
      Object val = method.invoke(startupDto);
      if (val instanceof Long l) {
        return l;
      }
      if (val instanceof Integer i) {
        return i.longValue();
      }
    } catch (Exception ignored) {
    }
    return null;
  }
}
