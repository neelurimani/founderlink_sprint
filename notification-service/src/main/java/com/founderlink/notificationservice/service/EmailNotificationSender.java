package com.founderlink.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender {

  private final ObjectProvider<JavaMailSender> mailSender;

  @Value("${founderlink.email.enabled:true}")
  private boolean enabled;

  /** Delivery mode: {@code log} (stdout, no SMTP) or {@code smtp} (requires JavaMailSender). */
  @Value("${founderlink.email.delivery:log}")
  private String delivery;

  @Value("${founderlink.email.from:no-reply@founderlink.local}")
  private String from;

  @Value("${founderlink.email.default-to:}")
  private String defaultTo;

  private boolean envEmailEnabled() {
    String v = System.getenv("FOUNDERLINK_EMAIL_ENABLED");
    return v != null && ("true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim()));
  }

  private String envDelivery() {
    String v = System.getenv("FOUNDERLINK_EMAIL_DELIVERY");
    return v == null ? null : v.trim();
  }

  public boolean isEnabled() {
    return enabled || envEmailEnabled();
  }

  private String effectiveDelivery() {
    String env = envDelivery();
    if (StringUtils.hasText(env)) {
      return env;
    }
    return delivery == null ? "log" : delivery.trim();
  }

  public boolean send(String to, String subject, String body) {
    if (!isEnabled()) {
      return false;
    }
    String resolvedTo = StringUtils.hasText(to) ? to : defaultTo;
    if (!StringUtils.hasText(resolvedTo)) {
      return false;
    }

    String mode = effectiveDelivery();
    if ("log".equalsIgnoreCase(mode)) {
      log.info(
          "[FounderLink email] from={} to={} subject={} body={}", from, resolvedTo, subject, body);
      return true;
    }

    JavaMailSender sender = mailSender.getIfAvailable();
    if (sender == null) {
      log.warn("founderlink.email.delivery=smtp but no JavaMailSender bean configured");
      return false;
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(resolvedTo);
    message.setSubject(subject);
    message.setText(body);
    sender.send(message);
    return true;
  }
}
