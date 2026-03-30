package com.founderlink.investmentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange/queue declarations + JSON {@link RabbitTemplate} so events match notification-service
 * consumers.
 */
@Configuration
public class RabbitMqConfig {

  public static final String FOUNDERLINK_EXCHANGE = "founderlink.exchange";
  public static final String INVESTMENT_CREATED_QUEUE = "investment.created.queue";
  public static final String INVESTMENT_CREATED_ROUTING_KEY = "investment.created";

  @Bean
  public TopicExchange founderLinkExchange() {
    return new TopicExchange(FOUNDERLINK_EXCHANGE, true, false);
  }

  @Bean
  public Queue investmentCreatedQueue() {
    return new Queue(INVESTMENT_CREATED_QUEUE, true);
  }

  @Bean
  public Binding investmentCreatedBinding(
      Queue investmentCreatedQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(investmentCreatedQueue)
        .to(founderLinkExchange)
        .with(INVESTMENT_CREATED_ROUTING_KEY);
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
