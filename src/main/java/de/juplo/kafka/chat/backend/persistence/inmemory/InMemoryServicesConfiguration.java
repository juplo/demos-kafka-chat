package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
public class InMemoryServicesConfiguration
{
  @Bean
  InMemoryChatHomeService chatHomeService(
      StorageStrategy storageStrategy,
      Clock clock,
      ChatBackendProperties properties)
  {
    return new InMemoryChatHomeService(
        storageStrategy.read(),
        clock,
        properties.getChatroomBufferSize());
  }
}
