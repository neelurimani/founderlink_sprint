package com.founderlink.notificationservice.client.fallback;

import com.founderlink.notificationservice.client.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationUserServiceClientFallbackFactory
    implements FallbackFactory<UserServiceClient> {

  @Override
  public UserServiceClient create(Throwable cause) {
    return userId -> {
      log.warn(
          "user-service Feign fallback (notification): userId={}, error={}",
          userId,
          cause != null ? cause.toString() : "unknown");
      return null;
    };
  }
}
