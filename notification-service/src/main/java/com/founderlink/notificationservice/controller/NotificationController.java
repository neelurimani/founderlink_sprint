package com.founderlink.notificationservice.controller;

import com.founderlink.notificationservice.dto.NotificationResponse;
import com.founderlink.notificationservice.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public List<NotificationResponse> all() {
    return notificationService.listAll();
  }

  @GetMapping("/user/{userId}")
  @PreAuthorize("isAuthenticated()")
  public List<NotificationResponse> byUser(@PathVariable Long userId) {
    return notificationService.listByRecipient(userId);
  }
}
