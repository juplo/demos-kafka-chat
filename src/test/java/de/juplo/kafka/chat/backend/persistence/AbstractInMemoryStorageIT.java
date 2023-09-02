package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.persistence.inmemory.SimpleChatHome;
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

      SimpleChatHome simpleChatHome = new SimpleChatHome(
          getStorageStrategy().read(),
          clock,
          bufferSize);

      @Override
      public ChatHome getChatHome()
      {
        return simpleChatHome;
      }
    };
  }
}
