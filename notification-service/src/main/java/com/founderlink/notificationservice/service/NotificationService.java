package com.founderlink.notificationservice.service;

import com.founderlink.notificationservice.client.StartupServiceClient;
import com.founderlink.notificationservice.client.UserServiceClient;
import com.founderlink.notificationservice.dto.NotificationResponse;
import com.founderlink.notificationservice.entity.Notification;
import com.founderlink.notificationservice.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final NotificationRepository repository;
  private final EmailNotificationSender emailSender;
  private final UserServiceClient userServiceClient;
  private final StartupServiceClient startupServiceClient;

  @Value("${founderlink.default-admin-id:1}")
  private Long defaultAdminId;

  @RabbitListener(
      queues = "notification.queue",
      autoStartup = "${spring.rabbitmq.listener.simple.auto-startup:true}")
  @CacheEvict(
      cacheNames = {"notificationsAll", "notificationsByRecipient"},
      allEntries = true)
  public void onEvent(
      Map<String, Object> event, @Header(name = "amqp_receivedRoutingKey") String routingKey) {
    log.info(
        "Notification consumer received routingKey={} payloadKeys={}",
        routingKey,
        event != null ? event.keySet() : null);

    String dedupeKey = buildDedupeKey(routingKey, event);
    if (dedupeKey != null && repository.existsByDedupeKey(dedupeKey)) {
      log.info("Skipping idempotent duplicate notification dedupeKey={}", dedupeKey);
      return;
    }

    Notification notification = new Notification();
    notification.setDeliveryChannel("EMAIL");
    notification.setStatus("QUEUED");
    Long recipientUserId = null;
    String recipientRole = null;
    String subject;
    if ("startup.created".equals(routingKey)) {
      recipientUserId = asLong(event.get("founderId"));
      recipientRole = "FOUNDER";
      notification.setType("STARTUP_CREATED");
      notification.setMessage(
          "Your startup was submitted and is pending review. startupId="
              + event.get("startupId")
              + ", industry="
              + event.get("industry"));
      subject = "FounderLink: Startup submitted";
    } else if ("startup.approved".equals(routingKey)) {
      recipientUserId = asLong(event.get("founderId"));
      recipientRole = "FOUNDER";
      notification.setType("STARTUP_APPROVED");
      notification.setMessage(
          "Your startup has been approved and is now visible on FounderLink. startupId="
              + event.get("startupId")
              + ", industry="
              + event.get("industry"));
      subject = "FounderLink: Startup approved";
    } else if ("investment.created".equals(routingKey)) {
      recipientRole = "FOUNDER";
      recipientUserId = asLong(event.get("founderId"));
      if (recipientUserId == null) {
        recipientUserId = resolveFounderIdFromStartup(event);
      }
      notification.setType("INVESTMENT_CREATED");
      notification.setMessage(
          "Investment request: startupId="
              + event.get("startupId")
              + ", amount="
              + event.get("amount"));
      subject = "FounderLink: New investment request";
    } else if ("investment.approved".equals(routingKey)) {
      recipientRole = "INVESTOR";
      recipientUserId = asLong(event.get("investorId"));
      notification.setType("INVESTMENT_APPROVED");
      notification.setMessage(
          "Investment approved: startupId="
              + event.get("startupId")
              + ", amount="
              + event.get("amount"));
      subject = "FounderLink: Investment approved";
    } else if ("team.invite.sent".equals(routingKey)) {
      recipientRole = "COFOUNDER";
      recipientUserId = asLong(event.get("invitedUserId"));
      notification.setType("TEAM_INVITE_SENT");
      notification.setMessage(
          "You were invited to join startupId="
              + event.get("startupId")
              + " as "
              + event.get("role"));
      subject = "FounderLink: Team invitation";
    } else {
      recipientRole = "SYSTEM";
      notification.setType("UNKNOWN_EVENT");
      notification.setMessage("Unhandled event for routing key: " + routingKey);
      subject = "FounderLink: Notification";
    }

    notification.setRecipientRole(recipientRole);
    notification.setRecipientUserId(recipientUserId);
    notification.setDedupeKey(dedupeKey);

    String recipientEmail = resolveRecipientEmail(event);
    if (!StringUtils.hasText(recipientEmail) && recipientUserId != null) {
      recipientEmail = resolveRecipientEmailFromUserId(recipientUserId);
    }
    notification.setRecipientEmail(recipientEmail);

    Notification saved;
    try {
      saved = repository.save(notification);
    } catch (DataIntegrityViolationException ex) {
      log.info(
          "Dedupe race: notification already persisted dedupeKey={} msg={}",
          dedupeKey,
          ex.getMostSpecificCause().getMessage());
      return;
    }

    try {
      boolean delivered = emailSender.send(saved.getRecipientEmail(), subject, saved.getMessage());
      saved.setStatus(delivered ? "SENT" : "QUEUED");
    } catch (Exception ex) {
      log.warn(
          "Email send failed: to={}, subject={}, type={}, err={}",
          saved.getRecipientEmail(),
          subject,
          saved.getType(),
          ex.toString());
      saved.setStatus("FAILED");
    }
    repository.save(saved);
    log.info(
        "Notification persisted id={} type={} routingKey={} status={}",
        saved.getId(),
        saved.getType(),
        routingKey,
        saved.getStatus());
  }

  private String buildDedupeKey(String routingKey, Map<String, Object> event) {
    if (event == null) {
      return null;
    }
    Long sid = asLong(event.get("startupId"));
    Long iid = asLong(event.get("investorId"));
    Long invited = asLong(event.get("invitedUserId"));
    if ("startup.created".equals(routingKey) && sid != null) {
      return "STARTUP_CREATED:" + sid;
    }
    if ("startup.approved".equals(routingKey) && sid != null) {
      return "STARTUP_APPROVED:" + sid;
    }
    if ("investment.created".equals(routingKey) && sid != null && iid != null) {
      return "INVESTMENT_CREATED:" + sid + ":" + iid;
    }
    if ("investment.approved".equals(routingKey) && sid != null && iid != null) {
      return "INVESTMENT_APPROVED:" + sid + ":" + iid;
    }
    if ("team.invite.sent".equals(routingKey) && sid != null && invited != null) {
      return "TEAM_INVITE:" + sid + ":" + invited;
    }
    return null;
  }

  @Cacheable(cacheNames = "notificationsAll", key = "'all'")
  public List<NotificationResponse> listAll() {
    requireAdmin();
    return repository.findAll().stream().map(this::toResponse).toList();
  }

  @Cacheable(cacheNames = "notificationsByRecipient", key = "#recipientUserId")
  public List<NotificationResponse> listByRecipient(Long recipientUserId) {
    Long callerUserId = requireCallerUserId();
    if (!isAdmin() && !callerUserId.equals(recipientUserId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You can only view your own notifications");
    }
    return repository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId).stream()
        .map(this::toResponse)
        .toList();
  }

  private NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getRecipientUserId(),
        notification.getRecipientEmail(),
        notification.getRecipientRole(),
        notification.getType(),
        notification.getMessage(),
        notification.getDeliveryChannel(),
        notification.getStatus(),
        notification.getCreatedAt());
  }

  private String resolveRecipientEmail(Map<String, Object> event) {
    String direct = asString(event.get("recipientEmail"));
    if (StringUtils.hasText(direct)) {
      return direct;
    }
    String email = asString(event.get("email"));
    if (StringUtils.hasText(email)) {
      return email;
    }
    return null;
  }

  private Long resolveFounderIdFromStartup(Map<String, Object> event) {
    Long startupId = asLong(event.get("startupId"));
    if (startupId == null) {
      return null;
    }
    try {
      return startupServiceClient.getStartupById(startupId).founderId();
    } catch (Exception ignored) {
      return null;
    }
  }

  private String resolveRecipientEmailFromUserId(Long userId) {
    try {
      var profile = userServiceClient.getUserById(userId);
      return profile == null ? null : profile.email();
    } catch (Exception ignored) {
      return null;
    }
  }

  private Long requireCallerUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && StringUtils.hasText(authentication.getName())) {
      try {
        return Long.valueOf(authentication.getName());
      } catch (NumberFormatException ignored) {
        // fall through to forwarded headers
      }
    }

    String userIdHeader = currentHeader("X-User-Id");
    if (StringUtils.hasText(userIdHeader)) {
      try {
        return Long.valueOf(userIdHeader.trim());
      } catch (NumberFormatException ignored) {
        // fall through
      }
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user id");
  }

  private boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getAuthorities() != null) {
      boolean adminByRole =
          authentication.getAuthorities().stream()
              .map(a -> a.getAuthority())
              .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
      if (adminByRole) {
        return true;
      }
      try {
        if (Long.valueOf(authentication.getName()).equals(defaultAdminId)) {
          return true;
        }
      } catch (Exception ignored) {
        // fall through to forwarded headers
      }
    }

    String rolesHeader = currentHeader("X-Roles");
    if (StringUtils.hasText(rolesHeader)) {
      String normalized = rolesHeader.toUpperCase();
      if (normalized.contains("ROLE_ADMIN") || normalized.contains("ADMIN")) {
        return true;
      }
    }

    Long callerId = null;
    String userIdHeader = currentHeader("X-User-Id");
    if (StringUtils.hasText(userIdHeader)) {
      try {
        callerId = Long.valueOf(userIdHeader.trim());
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    if (callerId != null && callerId.equals(defaultAdminId)) {
      return true;
    }
    return false;
  }

  private void requireAdmin() {
    if (!isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN access required");
    }
  }

  private Long asLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Long l) {
      return l;
    }
    if (value instanceof Integer i) {
      return i.longValue();
    }
    try {
      return Long.valueOf(String.valueOf(value));
    } catch (Exception ignored) {
      return null;
    }
  }

  private String asString(Object value) {
    if (value == null) {
      return null;
    }
    String s = String.valueOf(value);
    return StringUtils.hasText(s) ? s : null;
  }

  private String currentHeader(String name) {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs == null || attrs.getRequest() == null) {
        return null;
      }
      return attrs.getRequest().getHeader(name);
    } catch (Exception ignored) {
      return null;
    }
  }
}
