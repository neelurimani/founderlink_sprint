package com.founderlink.analyticsservice;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class AnalyticsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AnalyticsServiceApplication.class, args);
  }
}
