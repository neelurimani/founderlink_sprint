package com.founderlink.notificationservice.client.fallback;

import com.founderlink.notificationservice.client.StartupServiceClient;
import com.founderlink.notificationservice.client.view.StartupView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationStartupServiceClientFallbackFactory
    implements FallbackFactory<StartupServiceClient> {

  @Override
  public StartupServiceClient create(Throwable cause) {
    return startupId -> {
      log.warn(
          "startup-service Feign fallback (notification): startupId={}, error={}",
          startupId,
          cause != null ? cause.toString() : "unknown");
      return new StartupView(startupId, null);
    };
  }
}
