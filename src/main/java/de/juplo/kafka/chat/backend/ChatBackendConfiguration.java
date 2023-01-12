package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
@EnableConfigurationProperties(ChatBackendProperties.class)
public class ChatBackendConfiguration
{
  @Bean
  public ChatHome chatHome(ChatHomeService chatHomeService)
  {
    return new ChatHome(chatHomeService, 0);
  }

  @Bean
  public Clock clock()
  {
    return Clock.systemDefaultZone();
  }
}
