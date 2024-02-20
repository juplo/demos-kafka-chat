package de.juplo.kafka.chat.backend.storage.mongodb;

import de.juplo.kafka.chat.backend.implementation.ShardingStrategy;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


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
      MessageRepository messageRepository)
  {
    return new MongoDbStorageStrategy(chatRoomRepository, messageRepository);
  }
}
