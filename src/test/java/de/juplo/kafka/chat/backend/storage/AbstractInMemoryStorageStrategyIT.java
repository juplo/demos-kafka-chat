package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.implementation.inmemory.InMemoryTestUtils;
import de.juplo.kafka.chat.backend.implementation.inmemory.SimpleChatHomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;


@ContextConfiguration(classes = InMemoryTestUtils.class)
@Slf4j
public abstract class AbstractInMemoryStorageIT extends AbstractStorageStrategyIT
{
  @Autowired
  InMemoryTestUtils testUtils;
  @Autowired
  SimpleChatHomeService simpleChatHomeService;

  @Override
  void restore()
  {
    testUtils.restore(simpleChatHomeService).block();
  }
}
