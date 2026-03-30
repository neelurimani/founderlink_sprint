package com.founderlink.notificationservice.client;

import com.founderlink.notificationservice.client.fallback.NotificationStartupServiceClientFallbackFactory;
import com.founderlink.notificationservice.client.view.StartupView;
import com.founderlink.notificationservice.config.FeignInternalAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "startup-service",
    configuration = FeignInternalAuthConfig.class,
    fallbackFactory = NotificationStartupServiceClientFallbackFactory.class)
public interface StartupServiceClient {

  @GetMapping("/startups/{id}")
  StartupView getStartupById(@PathVariable("id") Long id);
}
