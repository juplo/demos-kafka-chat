package de.juplo.kafka.chat.backend.persistence.storage.mongodb;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.api.ShardingStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@ConditionalOnProperty(
    prefix = "chat.backend.inmemory",
    name = "storage-strategy",
    havingValue = "mongodb")
@Configuration
public class MongoDbStorageConfiguration
{
  @Bean
  public StorageStrategy storageStrategy(
      ChatRoomRepository chatRoomRepository,
      ChatBackendProperties properties,
      Clock clock,
      ShardingStrategy shardingStrategy)
  {
    return new MongoDbStorageStrategy(
        chatRoomRepository,
        clock,
        properties.getChatroomBufferSize(),
        shardingStrategy,
        messageFlux -> new InMemoryChatRoomService(messageFlux));
  }
}
