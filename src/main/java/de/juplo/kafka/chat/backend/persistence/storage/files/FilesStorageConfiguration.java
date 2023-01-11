package de.juplo.kafka.chat.backend.persistence.storage.files;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.time.Clock;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "storage",
    havingValue = "files",
    matchIfMissing = true)
@Configuration
public class FilesStorageConfiguration
{
  @Bean
  public StorageStrategy storageStrategy(
      ChatBackendProperties properties,
      Clock clock,
      ObjectMapper mapper)
  {
    return new FilesStorageStrategy(
        Paths.get(properties.getStorageDirectory()),
        clock,
        properties.getChatroomBufferSize(),
        messageFlux -> new InMemoryChatRoomService(messageFlux),
        mapper);
  }
}