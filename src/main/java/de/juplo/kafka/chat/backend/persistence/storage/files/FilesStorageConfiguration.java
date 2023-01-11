package de.juplo.kafka.chat.backend.persistence.storage.files;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.time.Clock;


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
