package com.founderlink.messagingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
  @NotBlank private String conversationId;
  @NotNull private Long senderId;
  @NotNull private Long receiverId;
  @NotBlank private String content;
}
