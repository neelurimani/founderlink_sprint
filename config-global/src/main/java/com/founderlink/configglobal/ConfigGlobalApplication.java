package com.founderlink.configglobal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigGlobalApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigGlobalApplication.class, args);
  }
}
