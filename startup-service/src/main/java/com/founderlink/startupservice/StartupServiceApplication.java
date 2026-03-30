package com.founderlink.startupservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class StartupServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(StartupServiceApplication.class, args);
  }
}
