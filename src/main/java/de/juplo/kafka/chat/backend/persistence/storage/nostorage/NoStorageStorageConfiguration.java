package de.juplo.kafka.chat.backend.persistence.storage.nostorage;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ShardingStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.nio.file.Paths;
import java.time.Clock;


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
      public void write(Flux<ChatRoom> chatroomFlux) {}

      @Override
      public Flux<ChatRoom> read()
      {
        return Flux.empty();
      }
    };
  }
}
