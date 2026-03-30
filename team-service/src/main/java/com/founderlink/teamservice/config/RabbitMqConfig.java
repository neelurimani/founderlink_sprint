package com.founderlink.teamservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the shared topic exchange and a JSON {@link RabbitTemplate} for domain events (e.g.
 * {@code team.invite.sent}).
 */
@Configuration
public class RabbitMqConfig {

  public static final String FOUNDERLINK_EXCHANGE = "founderlink.exchange";

  @Bean
  public TopicExchange founderLinkExchange() {
    return new TopicExchange(FOUNDERLINK_EXCHANGE, true, false);
  }

  @Bean
  public MessageConverter jacksonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jacksonMessageConverter);
    return template;
  }
}
