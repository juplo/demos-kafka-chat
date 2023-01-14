package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.api.KafkaLikeShardingStrategy;
import de.juplo.kafka.chat.backend.api.ShardingStrategy;
import de.juplo.kafka.chat.backend.domain.ChatHome;
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
  ChatHome[] chatHomes(
      ChatBackendProperties properties,
      InMemoryChatHomeService chatHomeService,
      StorageStrategy storageStrategy)
  {
    ChatHome[] chatHomes = new ChatHome[properties.getInmemory().getNumShards()];
    storageStrategy
        .read()
        .subscribe(chatRoom ->
        {
          int shard = chatRoom.getShard();
          if (chatHomes[shard] == null)
            chatHomes[shard] = new ChatHome(chatHomeService, shard);
        });
    return chatHomes;
  }

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
