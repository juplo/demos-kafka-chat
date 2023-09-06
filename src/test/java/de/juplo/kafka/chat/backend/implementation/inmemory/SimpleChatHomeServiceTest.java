package de.juplo.kafka.chat.backend.persistence.inmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.domain.ChatHomeServiceTest;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.storage.files.FilesStorageStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.file.Paths;
import java.time.Clock;


public class SimpleChatHomeServiceTest extends ChatHomeServiceTest
{
  @TestConfiguration
  static class Configuration
  {
    @Bean
    SimpleChatHomeService chatHome(
        StorageStrategy storageStrategy,
        Clock clock)
    {
      return new SimpleChatHomeService(
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
