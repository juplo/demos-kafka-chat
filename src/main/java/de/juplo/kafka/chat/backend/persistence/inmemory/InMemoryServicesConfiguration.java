package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.persistence.ShardingStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.stream.IntStream;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "services",
    havingValue = "inmemory",
    matchIfMissing = true)
@Configuration
public class InMemoryServicesConfiguration
{
  @Bean
  @ConditionalOnProperty(
      prefix = "chat.backend.inmemory",
      name = "sharding-strategy",
      havingValue = "none",
      matchIfMissing = true)
  ChatHome noneShardingChatHome(
      ChatBackendProperties properties,
      StorageStrategy storageStrategy,
      Clock clock)
  {
    return new SimpleChatHome(
        storageStrategy,
        clock,
        properties.getChatroomBufferSize());
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "chat.backend.inmemory",
      name = "sharding-strategy",
      havingValue = "kafkalike")
  ChatHome kafkalikeShardingChatHome(
      ChatBackendProperties properties,
      StorageStrategy storageStrategy,
      Clock clock)
  {
    int numShards = properties.getInmemory().getNumShards();
    SimpleChatHome[] chatHomes = new SimpleChatHome[numShards];
    IntStream
        .of(properties.getInmemory().getOwnedShards())
        .forEach(shard -> chatHomes[shard] = new SimpleChatHome(
            shard,
            storageStrategy,
            clock,
            properties.getChatroomBufferSize()));
    ShardingStrategy strategy = new KafkaLikeShardingStrategy(numShards);
    return new ShardedChatHome(chatHomes, strategy);
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
