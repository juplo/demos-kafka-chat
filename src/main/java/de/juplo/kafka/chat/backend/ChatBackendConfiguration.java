package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.domain.ChatroomFactory;
import de.juplo.kafka.chat.backend.persistence.InMemoryChatroomFactory;
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
  public ChatHome chatHome(
      ChatroomFactory chatroomFactory,
      StorageStrategy storageStrategy)
  {
    return new ChatHome(chatroomFactory, storageStrategy.readChatrooms());
  }

  @Bean
  public StorageStrategy storageStrategy(
      ChatBackendProperties properties,
      ObjectMapper mapper,
      ChatroomFactory chatroomFactory)
  {
    return new LocalJsonFilesStorageStrategy(
        Paths.get(properties.getDatadir()),
        mapper,
        chatroomFactory);
  }

  @Bean
  ChatroomFactory chatroomFactory(ChatBackendProperties properties)
  {
    return new InMemoryChatroomFactory(properties.getChatroomBufferSize());
  }

  @Bean
  public Clock clock()
  {
    return Clock.systemDefaultZone();
  }
}
