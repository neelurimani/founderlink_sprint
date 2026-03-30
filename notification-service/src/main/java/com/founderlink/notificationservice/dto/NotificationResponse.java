package com.founderlink.notificationservice.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    Long recipientUserId,
    String recipientEmail,
    String recipientRole,
    String type,
    String message,
    String deliveryChannel,
    String status,
    LocalDateTime createdAt) {}
