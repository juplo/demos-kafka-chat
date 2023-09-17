package de.juplo.kafka.chat.backend.storage.nostorage;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.UUID;


@ConditionalOnProperty(
    prefix = "chat.backend.inmemory",
    name = "storage-strategy",
    havingValue = "none",
    matchIfMissing = true)
@Configuration
@EnableAutoConfiguration(
    exclude = {
        MongoRepositoriesAutoConfiguration.class,
        MongoAutoConfiguration.class })
public class NoStorageStorageConfiguration
{
  @Bean
  public StorageStrategy storageStrategy()
  {
    return new StorageStrategy()
    {
      @Override
      public void writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux) {}

      @Override
      public Flux<ChatRoomInfo> readChatRoomInfo()
      {
        return Flux.empty();
      }

      @Override
      public void writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux) {}

      @Override
      public Flux<Message> readChatRoomData(UUID chatRoomId)
      {
        return Flux.empty();
      }
    };
  }
}