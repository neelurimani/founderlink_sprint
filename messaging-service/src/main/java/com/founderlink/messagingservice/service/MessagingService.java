package com.founderlink.messagingservice.service;

import com.founderlink.messagingservice.dto.ChatMessageResponse;
import com.founderlink.messagingservice.dto.SendMessageRequest;
import com.founderlink.messagingservice.entity.ChatMessage;
import com.founderlink.messagingservice.repository.ChatMessageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MessagingService {

  private final ChatMessageRepository repository;

  @CacheEvict(cacheNames = "conversations", key = "#request.conversationId")
  public ChatMessageResponse send(SendMessageRequest request) {
    Long callerUserId = requireCallerUserId();
    if (!callerUserId.equals(request.getSenderId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "senderId must match authenticated user");
    }

    ChatMessage message = new ChatMessage();
    message.setConversationId(request.getConversationId());
    message.setSenderId(request.getSenderId());
    message.setReceiverId(request.getReceiverId());
    message.setContent(request.getContent());
    ChatMessage saved = repository.save(message);
    return mapToResponse(saved);
  }

  public List<ChatMessageResponse> getConversation(String conversationId) {
    List<ChatMessage> messages = repository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    if (messages.isEmpty()) {
      return List.of();
    }

    Long callerUserId = requireCallerUserId();
    ChatMessage first = messages.get(0);
    Long participantA = first.getSenderId();
    Long participantB = first.getReceiverId();
    boolean isParticipant = callerUserId.equals(participantA) || callerUserId.equals(participantB);
    if (!isParticipant) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You are not allowed to view this conversation");
    }

    return messages.stream().map(this::mapToResponse).toList();
  }

  private ChatMessageResponse mapToResponse(ChatMessage message) {
    return new ChatMessageResponse(
        message.getId(),
        message.getConversationId(),
        message.getSenderId(),
        message.getReceiverId(),
        message.getContent(),
        message.getCreatedAt());
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
}
