package com.founderlink.messagingservice.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    String conversationId,
    Long senderId,
    Long receiverId,
    String content,
    LocalDateTime createdAt) {}
