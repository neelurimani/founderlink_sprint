package com.founderlink.investmentservice.client.fallback;

import com.founderlink.investmentservice.client.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

  static final String METHOD_KEY = "UserServiceClient#getUserById(Long)";

  @Override
  public UserServiceClient create(Throwable cause) {
    return investorId -> {
      log.error(
          "User service Feign fallback: investorId={}, error={}",
          investorId,
          cause != null ? cause.toString() : "unknown");
      throw FeignFallbackSupport.toFeignException(
          METHOD_KEY, "User service unreachable", cause);
    };
  }
}
