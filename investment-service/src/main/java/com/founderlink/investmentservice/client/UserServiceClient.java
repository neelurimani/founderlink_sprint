package com.founderlink.investmentservice.client;

import com.founderlink.investmentservice.client.fallback.UserServiceClientFallbackFactory;
import com.founderlink.investmentservice.client.view.UserProfileView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallbackFactory = UserServiceClientFallbackFactory.class)
public interface UserServiceClient {

  @GetMapping("/users/{id}")
  UserProfileView getUserById(@PathVariable("id") Long id);
}
