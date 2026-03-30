package com.founderlink.investmentservice.client;

import com.founderlink.investmentservice.client.fallback.StartupServiceClientFallbackFactory;
import com.founderlink.investmentservice.client.view.StartupView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "startup-service",
    fallbackFactory = StartupServiceClientFallbackFactory.class)
public interface StartupServiceClient {

  @GetMapping("/startups/{id}")
  StartupView getStartupById(@PathVariable("id") Long id);
}
