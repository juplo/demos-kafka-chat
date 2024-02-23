package de.juplo.kafka.chat.backend.storage.nostorage;

import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@ConditionalOnProperty(
    prefix = "chat.backend.inmemory",
    name = "storage-strategy",
    havingValue = "none",
    matchIfMissing = true)
@Configuration
public class NoStorageStorageConfiguration
{
  @Bean
  public StorageStrategy storageStrategy()
  {
    return new NoStorageStorageStrategy();
  }
}
