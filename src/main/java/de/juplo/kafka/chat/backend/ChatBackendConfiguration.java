package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.persistence.InMemoryChatHomeService;
import de.juplo.kafka.chat.backend.persistence.LocalJsonFilesStorageStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.time.Clock;


@Configuration
@EnableConfigurationProperties(ChatBackendProperties.class)
public class ChatBackendConfiguration
{
  @Bean
  public ChatHome chatHome(ChatHomeService chatHomeService)
  {
    return new ChatHome(chatHomeService);
  }

  @Bean
  InMemoryChatHomeService chatHomeService(
      StorageStrategy storageStrategy,
      Clock clock,
      ChatBackendProperties properties)
  {
    return new InMemoryChatHomeService(
        storageStrategy.readChatrooms(),
        clock,
        properties.getChatroomBufferSize());
  }

  @Bean
  public StorageStrategy storageStrategy(
      ChatBackendProperties properties,
      Clock clock,
      ObjectMapper mapper)
  {
    return new LocalJsonFilesStorageStrategy(
        Paths.get(properties.getStorageDirectory()),
        clock,
        properties.getChatroomBufferSize(),
        mapper);
  }

  @Bean
  public Clock clock()
  {
    return Clock.systemDefaultZone();
  }
}
