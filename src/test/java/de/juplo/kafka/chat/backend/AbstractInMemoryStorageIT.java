package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.implementation.inmemory.SimpleChatHomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;


@RequiredArgsConstructor
@Slf4j
public abstract class AbstractInMemoryStorageIT extends AbstractStorageStrategyIT
{
  final Clock clock;

  @Override
  protected StorageStrategyITConfig getConfig()
  {
    return new StorageStrategyITConfig()
    {
      int bufferSize = 8;

      SimpleChatHomeService simpleChatHome = new SimpleChatHomeService(
          getStorageStrategy(),
          clock,
          bufferSize);

      @Override
      public ChatHomeService getChatHome()
      {
        return simpleChatHome;
      }
    };
  }
}
