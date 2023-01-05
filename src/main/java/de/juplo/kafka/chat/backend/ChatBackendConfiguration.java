package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.domain.ChatroomFactory;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.persistence.InMemoryChatroomFactory;
import de.juplo.kafka.chat.backend.persistence.InMemoryPersistenceStrategy;
import de.juplo.kafka.chat.backend.persistence.LocalJsonFilesStorageStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.time.Clock;
import java.util.LinkedHashMap;


@Configuration
@EnableConfigurationProperties(ChatBackendProperties.class)
public class ChatBackendConfiguration
{
  @Bean
  public ChatHome chatHome(
      ChatroomFactory chatroomFactory,
      StorageStrategy storageStrategy)
  {
    return new ChatHome(
        storageStrategy.readChatrooms().collectMap(chatroom -> chatroom.getId()).block(),
        chatroomFactory);
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
  ChatroomFactory chatroomFactory(InMemoryPersistenceStrategy persistenceStrategy)
  {
    return new InMemoryChatroomFactory(persistenceStrategy);
  }

  @Bean
  InMemoryPersistenceStrategy persistenceStrategy()
  {
    return new InMemoryPersistenceStrategy(new LinkedHashMap<>());
  }

  @Bean
  public Clock clock()
  {
    return Clock.systemDefaultZone();
  }
}
