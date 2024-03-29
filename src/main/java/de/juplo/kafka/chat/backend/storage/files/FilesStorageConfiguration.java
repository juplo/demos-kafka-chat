package de.juplo.kafka.chat.backend.storage.files;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.implementation.ShardingStrategy;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;


@ConditionalOnProperty(
    prefix = "chat.backend.inmemory",
    name = "storage-strategy",
    havingValue = "files")
@Configuration
public class FilesStorageConfiguration
{
  @Bean
  public StorageStrategy storageStrategy(
      ChatBackendProperties properties,
      ShardingStrategy shardingStrategy,
      ObjectMapper mapper)
  {
    return new FilesStorageStrategy(
        Paths.get(properties.getInmemory().getStorageDirectory()),
        shardingStrategy,
        mapper,
        properties.getProjectreactor().getLoggingLevel(),
        properties.getProjectreactor().isShowOperatorLine());
  }
}
