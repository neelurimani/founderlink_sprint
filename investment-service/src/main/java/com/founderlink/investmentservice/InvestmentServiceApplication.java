package com.founderlink.investmentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class InvestmentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(InvestmentServiceApplication.class, args);
  }
}
