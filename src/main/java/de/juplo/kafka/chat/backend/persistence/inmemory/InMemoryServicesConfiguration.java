package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.ChatBackendProperties.ShardingStrategyType;
import de.juplo.kafka.chat.backend.domain.ChatHome;
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
  ChatHome noneShardingChatHome(InMemoryChatHomeService chatHomeService)
  {
    return new SimpleChatHome(chatHomeService);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "chat.backend.inmemory",
      name = "sharding-strategy",
      havingValue = "kafkalike")
  ChatHome kafkalikeShardingChatHome(
      ChatBackendProperties properties,
      InMemoryChatHomeService chatHomeService)
  {
    int numShards = properties.getInmemory().getNumShards();
    SimpleChatHome[] chatHomes = new SimpleChatHome[numShards];
    IntStream
        .of(properties.getInmemory().getOwnedShards())
        .forEach(shard -> chatHomes[shard] = new SimpleChatHome(chatHomeService, shard));
    ShardingStrategy strategy = new KafkaLikeShardingStrategy(numShards);
    return new ShardedChatHome(chatHomes, strategy);
  }

  @Bean
  InMemoryChatHomeService chatHomeService(
      ChatBackendProperties properties,
      StorageStrategy storageStrategy)
  {
    ShardingStrategyType sharding =
        properties.getInmemory().getShardingStrategy();
    int numShards = sharding == ShardingStrategyType.none
        ? 1
        : properties.getInmemory().getNumShards();
    int[] ownedShards = sharding == ShardingStrategyType.none
        ? new int[] { 0 }
        : properties.getInmemory().getOwnedShards();
    return new InMemoryChatHomeService(
        numShards,
        ownedShards,
        storageStrategy.read());
  }

  @Bean
  InMemoryChatRoomFactory chatRoomFactory(
      InMemoryChatHomeService service,
      ShardingStrategy strategy,
      Clock clock,
      ChatBackendProperties properties)
  {
    return new InMemoryChatRoomFactory(
        service,
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
