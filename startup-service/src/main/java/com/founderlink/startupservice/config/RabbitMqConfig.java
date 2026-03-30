package com.founderlink.startupservice.config;

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

@Configuration
public class RabbitMqConfig {

  public static final String FOUNDERLINK_EXCHANGE = "founderlink.exchange";
  public static final String STARTUP_CREATED_QUEUE = "startup.created.queue";
  public static final String STARTUP_CREATED_ROUTING_KEY = "startup.created";

  @Bean
  public TopicExchange founderLinkExchange() {
    return new TopicExchange(FOUNDERLINK_EXCHANGE, true, false);
  }

  @Bean
  public Queue startupCreatedQueue() {
    return new Queue(STARTUP_CREATED_QUEUE, true);
  }

  @Bean
  public Binding startupCreatedBinding(
      Queue startupCreatedQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(startupCreatedQueue)
        .to(founderLinkExchange)
        .with(STARTUP_CREATED_ROUTING_KEY);
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
