package de.juplo.kafka.chat.backend.persistence.inmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.domain.ChatHomeTest;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.storage.files.FilesStorageStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.file.Paths;
import java.time.Clock;


public class SimpleChatHomeTest extends ChatHomeTest
{
  @TestConfiguration
  static class Configuration
  {
    @Bean
    SimpleChatHome chatHome(
        StorageStrategy storageStrategy,
        Clock clock)
    {
      return new SimpleChatHome(
          storageStrategy,
          clock,
          bufferSize());
    }

    @Bean
    public FilesStorageStrategy storageStrategy(Clock clock)
    {
      return new FilesStorageStrategy(
          Paths.get("target", "test-classes", "data", "files"),
          chatRoomId -> 0,
          new ObjectMapper());
    }

    @Bean
    Clock clock()
    {
      return Clock.systemDefaultZone();
    }

    int bufferSize()
    {
      return 8;
    }
  }
}
