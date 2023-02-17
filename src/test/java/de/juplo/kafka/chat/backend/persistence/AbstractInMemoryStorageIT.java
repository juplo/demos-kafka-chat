package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomFactory;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatHomeService;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomFactory;
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
      InMemoryChatHomeService chatHomeService = new InMemoryChatHomeService(
          1,
          new int[] { 0 },
          getStorageStrategy().read());

      InMemoryChatRoomFactory chatRoomFactory = new InMemoryChatRoomFactory(
          chatRoomId -> 0,
          clock,
          8);

      @Override
      public ChatHomeService getChatHomeService()
      {
        return chatHomeService;
      }

      @Override
      public ChatRoomFactory getChatRoomFactory()
      {
        return chatRoomFactory;
      }
    };
  }
}
