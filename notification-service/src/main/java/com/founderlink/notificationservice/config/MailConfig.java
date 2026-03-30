package com.founderlink.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@ConditionalOnProperty(name = "founderlink.email.delivery", havingValue = "smtp")
public class MailConfig {

  @Bean
  @ConditionalOnMissingBean(JavaMailSender.class)
  public JavaMailSender javaMailSender(
      @Value("${spring.mail.host:localhost}") String host,
      @Value("${spring.mail.port:25}") int port,
      @Value("${spring.mail.username:}") String username,
      @Value("${spring.mail.password:}") String password) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(username);
    sender.setPassword(password);
    return sender;
  }
}
