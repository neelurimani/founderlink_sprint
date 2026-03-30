package com.founderlink.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

  public static final String FOUNDERLINK_EXCHANGE = "founderlink.exchange";
  public static final String NOTIFICATION_QUEUE = "notification.queue";

  @Bean
  public TopicExchange founderLinkExchange() {
    return new TopicExchange(FOUNDERLINK_EXCHANGE, true, false);
  }

  @Bean
  public Queue notificationQueue() {
    return new Queue(NOTIFICATION_QUEUE, true);
  }

  @Bean
  public Binding startupCreatedBinding(Queue notificationQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(notificationQueue).to(founderLinkExchange).with("startup.created");
  }

  @Bean
  public Binding startupApprovedBinding(
      Queue notificationQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(notificationQueue).to(founderLinkExchange).with("startup.approved");
  }

  @Bean
  public Binding investmentCreatedBinding(
      Queue notificationQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(notificationQueue)
        .to(founderLinkExchange)
        .with("investment.created");
  }

  @Bean
  public Binding investmentApprovedBinding(
      Queue notificationQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(notificationQueue)
        .to(founderLinkExchange)
        .with("investment.approved");
  }

  @Bean
  public Binding teamInviteSentBinding(Queue notificationQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(notificationQueue).to(founderLinkExchange).with("team.invite.sent");
  }

  /**
   * Aligns with producers using {@link Jackson2JsonMessageConverter} so {@code Map} payloads
   * deserialize correctly.
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter);
    return factory;
  }
}
