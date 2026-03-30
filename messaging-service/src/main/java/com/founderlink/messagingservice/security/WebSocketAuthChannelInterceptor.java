package com.founderlink.messagingservice.security;

import io.jsonwebtoken.Claims;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

  private final JwtSupport jwtSupport;

  public WebSocketAuthChannelInterceptor(JwtSupport jwtSupport) {
    this.jwtSupport = jwtSupport;
  }

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String token = resolveToken(accessor);
      if (!StringUtils.hasText(token)) {
        throw new IllegalArgumentException("Missing JWT token for websocket connect");
      }
      Claims claims = jwtSupport.parseToken(token);
      String userId = claims.get("userId", String.class);
      if (!StringUtils.hasText(userId)) {
        userId = claims.getSubject();
      }
      if (!StringUtils.hasText(userId)) {
        throw new IllegalArgumentException("Invalid JWT principal");
      }
      accessor.setUser(new StompPrincipal(userId));
      accessor.getSessionAttributes().put("userId", userId);
      accessor.getSessionAttributes().put("roles", String.valueOf(claims.get("roles")));
    }

    if (StompCommand.SEND.equals(accessor.getCommand())) {
      Object payload = message.getPayload();
      Map<?, ?> sessionAttributes = accessor.getSessionAttributes();
      String userId =
          sessionAttributes == null ? null : String.valueOf(sessionAttributes.get("userId"));
      if (payload instanceof byte[] bytes) {
        String body = new String(bytes);
        if (!body.contains("\"senderId\":" + userId)
            && !body.contains("\"senderId\":\"" + userId + "\"")) {
          throw new IllegalArgumentException("senderId must match authenticated user");
        }
      }
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      String destination = accessor.getDestination();
      if (!StringUtils.hasText(destination) || !destination.startsWith("/topic/conversations/")) {
        throw new IllegalArgumentException("Invalid websocket subscription destination");
      }
    }

    return message;
  }

  private String resolveToken(StompHeaderAccessor accessor) {
    List<String> authHeaders = accessor.getNativeHeader("Authorization");
    if (authHeaders != null && !authHeaders.isEmpty()) {
      String token = jwtSupport.extractBearerToken(authHeaders.get(0));
      if (StringUtils.hasText(token)) {
        return token;
      }
    }
    List<String> tokenHeaders = accessor.getNativeHeader("token");
    if (tokenHeaders != null
        && !tokenHeaders.isEmpty()
        && StringUtils.hasText(tokenHeaders.get(0))) {
      return tokenHeaders.get(0);
    }
    return null;
  }

  private static final class StompPrincipal implements Principal {
    private final String name;

    private StompPrincipal(String name) {
      this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
      return name;
    }
  }
}
