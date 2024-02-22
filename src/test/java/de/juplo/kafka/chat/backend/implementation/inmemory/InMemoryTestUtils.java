package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;


public class InMemoryTestUtils
{
  private final InMemoryServicesConfiguration config =
      new InMemoryServicesConfiguration();

  @Autowired
  ChatBackendProperties properties;
  @Autowired
  StorageStrategy storageStrategy;
  @Autowired
  Clock clock;


  public ChatHomeService createNoneShardingChatHomeService()
  {
    return config.noneShardingChatHome(
        properties,
        storageStrategy,
        clock);
  }
}
