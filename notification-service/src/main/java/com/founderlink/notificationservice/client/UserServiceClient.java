package com.founderlink.notificationservice.client;

import com.founderlink.notificationservice.client.fallback.NotificationUserServiceClientFallbackFactory;
import com.founderlink.notificationservice.client.view.UserProfileView;
import com.founderlink.notificationservice.config.FeignInternalAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "user-service",
    configuration = FeignInternalAuthConfig.class,
    fallbackFactory = NotificationUserServiceClientFallbackFactory.class)
public interface UserServiceClient {

  @GetMapping("/users/{id}")
  UserProfileView getUserById(@PathVariable("id") Long id);
}
