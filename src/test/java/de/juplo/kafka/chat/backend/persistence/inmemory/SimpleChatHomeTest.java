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
    SimpleChatHome chatHome(InMemoryChatHomeService chatHomeService)
    {
      return new SimpleChatHome(chatHomeService);
    }

    @Bean
    InMemoryChatHomeService chatHomeService(StorageStrategy storageStrategy)
    {
      return new InMemoryChatHomeService(
          1,
          new int[] { 0 },
          storageStrategy.read());
    }

    @Bean
    public FilesStorageStrategy storageStrategy()
    {
      return new FilesStorageStrategy(
          Paths.get("target", "test-classes", "data", "files"),
          Clock.systemDefaultZone(),
          8,
          chatRoomId -> 0,
          messageFlux -> new InMemoryChatRoomService(messageFlux),
          new ObjectMapper());
    }
  }
}
