package com.founderlink.apigateway.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfiguration {

  private static final Logger log = LoggerFactory.getLogger(WebClientConfiguration.class);

  @Bean
  WebClient.Builder webClientBuilder() {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofSeconds(60));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .filter(gatewayTraceFilter());
  }

  private static ExchangeFilterFunction gatewayTraceFilter() {
    return ExchangeFilterFunction.ofRequestProcessor(
        clientRequest -> {
          log.debug("Gateway downstream {} {}", clientRequest.method(), clientRequest.url());
          return Mono.just(clientRequest);
        });
  }
}
