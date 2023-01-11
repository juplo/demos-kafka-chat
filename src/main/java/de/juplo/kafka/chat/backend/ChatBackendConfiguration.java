package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.time.Clock;


@Configuration
@EnableConfigurationProperties(ChatBackendProperties.class)
@EnableReactiveMongoRepositories
public class ChatBackendConfiguration
{
  @Bean
  public ChatHome chatHome(ChatHomeService chatHomeService)
  {
    return new ChatHome(chatHomeService);
  }

  @Bean
  public Clock clock()
  {
    return Clock.systemDefaultZone();
  }
}
