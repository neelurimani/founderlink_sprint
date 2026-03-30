package com.founderlink.investmentservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignAuthRequestInterceptor implements RequestInterceptor {

  @Override
  public void apply(RequestTemplate template) {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return;
    }

    HttpServletRequest httpServletRequest = attributes.getRequest();

    String authorization = httpServletRequest.getHeader("Authorization");
    if (StringUtils.hasText(authorization)) {
      template.header("Authorization", authorization);
    }

    String userId = httpServletRequest.getHeader("X-User-Id");
    if (StringUtils.hasText(userId)) {
      template.header("X-User-Id", userId);
    }

    String roles = httpServletRequest.getHeader("X-Roles");
    if (StringUtils.hasText(roles)) {
      template.header("X-Roles", roles);
    }
  }
}
