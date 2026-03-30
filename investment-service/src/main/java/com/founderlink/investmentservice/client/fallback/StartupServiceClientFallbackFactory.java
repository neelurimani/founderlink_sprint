package com.founderlink.investmentservice.client.fallback;

import com.founderlink.investmentservice.client.StartupServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupServiceClientFallbackFactory implements FallbackFactory<StartupServiceClient> {

  static final String METHOD_KEY = "StartupServiceClient#getStartupById(Long)";

  @Override
  public StartupServiceClient create(Throwable cause) {
    return startupId -> {
      log.error(
          "Startup service Feign fallback: startupId={}, error={}",
          startupId,
          cause != null ? cause.toString() : "unknown");
      throw FeignFallbackSupport.toFeignException(
          METHOD_KEY, "Startup service unreachable", cause);
    };
  }
}
