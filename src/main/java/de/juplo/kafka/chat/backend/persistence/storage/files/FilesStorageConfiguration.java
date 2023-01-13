package de.juplo.kafka.chat.backend.persistence.storage.files;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.api.ShardingStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.time.Clock;


@ConditionalOnProperty(
    prefix = "chat.backend.inmemory",
    name = "storage-strategy",
    havingValue = "files",
    matchIfMissing = true)
@Configuration
@EnableAutoConfiguration(
    exclude = {
        MongoRepositoriesAutoConfiguration.class,
        MongoAutoConfiguration.class })
public class FilesStorageConfiguration
{
  @Bean
  public StorageStrategy storageStrategy(
      ChatBackendProperties properties,
      Clock clock,
      ShardingStrategy shardingStrategy,
      ObjectMapper mapper)
  {
    return new FilesStorageStrategy(
        Paths.get(properties.getInmemory().getStorageDirectory()),
        clock,
        properties.getChatroomBufferSize(),
        shardingStrategy,
        messageFlux -> new InMemoryChatRoomService(messageFlux),
        mapper);
  }
}
