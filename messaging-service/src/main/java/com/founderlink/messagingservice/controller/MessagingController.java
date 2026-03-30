package com.founderlink.messagingservice.controller;

import com.founderlink.messagingservice.dto.ChatMessageResponse;
import com.founderlink.messagingservice.dto.SendMessageRequest;
import com.founderlink.messagingservice.service.MessagingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessagingController {

  private final MessagingService messagingService;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ChatMessageResponse send(@Valid @RequestBody SendMessageRequest request) {
    return messagingService.send(request);
  }

  @GetMapping("/conversation/{id}")
  @PreAuthorize("isAuthenticated()")
  public List<ChatMessageResponse> conversation(@PathVariable("id") String conversationId) {
    return messagingService.getConversation(conversationId);
  }
}
