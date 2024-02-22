package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import de.juplo.kafka.chat.backend.implementation.inmemory.InMemoryServicesConfiguration;
import de.juplo.kafka.chat.backend.implementation.inmemory.InMemoryTestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.time.Clock;


@ContextConfiguration(classes = InMemoryTestUtils.class)
@Slf4j
public abstract class AbstractInMemoryStorageIT extends AbstractStorageStrategyIT
{
  @Autowired
  InMemoryTestUtils testUtils;

  @Override
  ChatHomeService getChatHome()
  {
    return testUtils.createNoneShardingChatHomeService();
  }
}
