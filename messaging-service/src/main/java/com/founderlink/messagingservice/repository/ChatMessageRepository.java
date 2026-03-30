package com.founderlink.messagingservice.repository;

import com.founderlink.messagingservice.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
