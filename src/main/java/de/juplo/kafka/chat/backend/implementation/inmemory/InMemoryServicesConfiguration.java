package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.implementation.ShardingStrategy;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
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
  ChatHomeService noneShardingChatHome(
      ChatBackendProperties properties,
      StorageStrategy storageStrategy,
      Clock clock)
  {
    SimpleChatHomeService chatHomeService = new SimpleChatHomeService(
        clock,
        properties.getChatroomBufferSize());
    chatHomeService.restore(storageStrategy).block();
    return chatHomeService;
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "chat.backend.inmemory",
      name = "sharding-strategy",
      havingValue = "kafkalike")
  ChatHomeService kafkalikeShardingChatHome(
      ChatBackendProperties properties,
      StorageStrategy storageStrategy,
      Clock clock)
  {
    int numShards = properties.getInmemory().getNumShards();
    SimpleChatHomeService[] chatHomes = new SimpleChatHomeService[numShards];
    IntStream
        .of(properties.getInmemory().getOwnedShards())
        .forEach(shard ->
        {
          SimpleChatHomeService service = chatHomes[shard] = new SimpleChatHomeService(
              shard,
              clock,
              properties.getChatroomBufferSize());
          service.restore(storageStrategy).block();
        });
    ShardingStrategy strategy = new KafkaLikeShardingStrategy(numShards);
    return new ShardedChatHomeService(
        properties.getInstanceId(),
        chatHomes,
        properties.getInmemory().getShardOwners(),
        strategy);
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
