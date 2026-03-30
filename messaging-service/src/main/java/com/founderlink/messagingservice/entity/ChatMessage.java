package com.founderlink.messagingservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String conversationId;
  private Long senderId;
  private Long receiverId;
  private String content;
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
