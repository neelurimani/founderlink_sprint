package com.founderlink.notificationservice.repository;

import com.founderlink.notificationservice.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

  boolean existsByDedupeKey(String dedupeKey);
}
