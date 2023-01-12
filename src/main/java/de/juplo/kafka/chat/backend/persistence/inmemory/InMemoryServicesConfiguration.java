package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.api.ShardingStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "services",
    havingValue = "in-memory",
    matchIfMissing = true)
@Configuration
public class InMemoryServicesConfiguration
{
  @Bean
  InMemoryChatHomeService chatHomeService(StorageStrategy storageStrategy)
  {
    return new InMemoryChatHomeService(1, storageStrategy.read());
  }

  @Bean
  InMemoryChatRoomFactory chatRoomFactory(
      ShardingStrategy strategy,
      Clock clock,
      ChatBackendProperties properties)
  {
    return new InMemoryChatRoomFactory(
        strategy,
        clock,
        properties.getChatroomBufferSize());
  }

  @Bean
  ShardingStrategy shardingStrategy()
  {
    return chatRoomId -> 0;
  }
}
