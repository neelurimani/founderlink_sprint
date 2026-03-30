package com.founderlink.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "notifications",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_dedupe_key", columnNames = "dedupe_key"))
@Getter
@Setter
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long recipientUserId;
  private String recipientEmail;
  private String recipientRole;
  private String type;
  private String message;
  private String deliveryChannel;
  private String status;
  private LocalDateTime createdAt;

  /**
   * Idempotency: e.g. INVESTMENT_CREATED:startupId:investorId — prevents duplicate notifications
   * from redelivered events.
   */
  @Column(name = "dedupe_key", length = 256)
  private String dedupeKey;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
