package de.juplo.kafka.chat.backend;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
@EnableConfigurationProperties(ChatBackendProperties.class)
public class ChatBackendConfiguration
{
  @Bean
  Clock clock()
  {
    return Clock.systemDefaultZone();
  }
}
