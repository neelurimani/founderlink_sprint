package com.founderlink.configglobal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(
    properties = {
      "spring.profiles.active=git",
      // Use local clone path for tests so they don't require GitHub credentials.
      "spring.cloud.config.server.git.uri=file:/Users/manineeluri/Desktop/FounderLink Global /.tmp-FounderLinkGlobal-config",
      "spring.cloud.config.server.git.clone-on-start=false"
    })
class ConfigGlobalApplicationTests {

  @Test
  void contextLoads() {}
}
