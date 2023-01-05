package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatroomFactory;
import de.juplo.kafka.chat.backend.persistence.InMemoryChatroomFactory;
import de.juplo.kafka.chat.backend.persistence.InMemoryPersistenceStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
public class ChatBackendConfiguration
{
  @Bean
  public ChatHome chatHome(ChatroomFactory chatroomFactory)
  {
    return new ChatHome(chatroomFactory);
  }

  @Bean
  ChatroomFactory chatroomFactory(InMemoryPersistenceStrategy persistenceStrategy)
  {
    return new InMemoryChatroomFactory(persistenceStrategy);
  }

  @Bean
  InMemoryPersistenceStrategy persistenceStrategy()
  {
    return new InMemoryPersistenceStrategy();
  }

  @Bean
  public Clock clock()
  {
    return Clock.systemDefaultZone();
  }
}
