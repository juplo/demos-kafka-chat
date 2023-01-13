package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.api.KafkaLikeShardingStrategy;
import de.juplo.kafka.chat.backend.api.ShardingStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "services",
    havingValue = "inmemory",
    matchIfMissing = true)
@Configuration
public class InMemoryServicesConfiguration
{
  @Bean
  InMemoryChatHomeService chatHomeService(
      ChatBackendProperties properties,
      StorageStrategy storageStrategy)
  {
    return new InMemoryChatHomeService(
        properties.getInmemory().getNumShards(),
        properties.getInmemory().getOwnedShards(),
        storageStrategy.read());
  }

  @Bean
  InMemoryChatHomeFactory chatHomeFactory(InMemoryChatHomeService service)
  {
    return new InMemoryChatHomeFactory(service);
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

  @ConditionalOnProperty(
      prefix = "chat.backend.inmemory",
      name = "sharding-strategy",
      havingValue = "none",
      matchIfMissing = true)
  @Bean
  ShardingStrategy defaultShardingStrategy()
  {
    return chatRoomId -> 0;
  }

  @ConditionalOnProperty(
      prefix = "chat.backend.inmemory",
      name = "sharding-strategy",
      havingValue = "kafkalike")
  @Bean
  ShardingStrategy kafkalikeShardingStrategy(ChatBackendProperties properties)
  {
    return new KafkaLikeShardingStrategy(
        properties.getInmemory().getNumShards());
  }
}
