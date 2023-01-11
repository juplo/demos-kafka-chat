package de.juplo.kafka.chat.backend.persistence.storage.mongodb;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "storage",
    havingValue = "mongodb")
@Configuration
public class MongoDbStorageConfiguration
{
  @Bean
  public StorageStrategy storageStrategy(
      ChatRoomRepository chatRoomRepository,
      ChatBackendProperties properties,
      Clock clock)
  {
    return new MongoDbStorageStrategy(
        chatRoomRepository,
        clock,
        properties.getChatroomBufferSize(),
        messageFlux -> new InMemoryChatRoomService(messageFlux));
  }
}
