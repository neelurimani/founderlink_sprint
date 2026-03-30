package com.founderlink.analyticsservice.config;

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
  public static final String ANALYTICS_QUEUE = "analytics.queue";

  @Bean
  public TopicExchange founderLinkExchange() {
    return new TopicExchange(FOUNDERLINK_EXCHANGE, true, false);
  }

  @Bean
  public Queue analyticsQueue() {
    return new Queue(ANALYTICS_QUEUE, true);
  }

  @Bean
  public Binding analyticsStartupCreated(Queue analyticsQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(analyticsQueue).to(founderLinkExchange).with("startup.created");
  }

  @Bean
  public Binding analyticsStartupApproved(Queue analyticsQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(analyticsQueue).to(founderLinkExchange).with("startup.approved");
  }

  @Bean
  public Binding analyticsInvestmentCreated(
      Queue analyticsQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(analyticsQueue).to(founderLinkExchange).with("investment.created");
  }

  @Bean
  public Binding analyticsInvestmentApproved(
      Queue analyticsQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(analyticsQueue).to(founderLinkExchange).with("investment.approved");
  }

  @Bean
  public Binding analyticsTeamInviteSent(Queue analyticsQueue, TopicExchange founderLinkExchange) {
    return BindingBuilder.bind(analyticsQueue).to(founderLinkExchange).with("team.invite.sent");
  }

  /**
   * Align with producers using {@link Jackson2JsonMessageConverter} so Map payloads deserialize
   * correctly.
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
