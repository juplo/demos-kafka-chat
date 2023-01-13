package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatHomeFactory;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
@EnableConfigurationProperties(ChatBackendProperties.class)
public class ChatBackendConfiguration
{
  @Bean
  ChatHome[] chatHomes(
      ChatHomeFactory factory,
      ChatBackendProperties properties,
      StorageStrategy storageStrategy)
  {
    ChatHome[] chatHomes = new ChatHome[properties.getInmemory().getNumShards()];
    storageStrategy
        .read()
        .subscribe(chatRoom ->
        {
          int shard = chatRoom.getShard();
          if (chatHomes[shard] == null)
            chatHomes[shard] = factory.createChatHome(shard);
        });
    return chatHomes;
  }

  @Bean
  Clock clock()
  {
    return Clock.systemDefaultZone();
  }
}
