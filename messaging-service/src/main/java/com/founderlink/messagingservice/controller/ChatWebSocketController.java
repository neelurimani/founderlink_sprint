package com.founderlink.messagingservice.controller;

import com.founderlink.messagingservice.dto.ChatMessageResponse;
import com.founderlink.messagingservice.dto.SendMessageRequest;
import com.founderlink.messagingservice.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

  private final MessagingService messagingService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat.send")
  public void send(@Valid SendMessageRequest request) {
    ChatMessageResponse saved = messagingService.send(request);
    messagingTemplate.convertAndSend("/topic/conversations/" + saved.conversationId(), saved);
  }
}
