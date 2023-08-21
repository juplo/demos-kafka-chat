package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatRoomFactory;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatHomeService;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomFactory;
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
      InMemoryChatHomeService inMemoryChatHomeService = new InMemoryChatHomeService(
          1,
          new int[] { 0 },
          getStorageStrategy().read());

      SimpleChatHome simpleChatHome = new SimpleChatHome(inMemoryChatHomeService);

      InMemoryChatRoomFactory chatRoomFactory = new InMemoryChatRoomFactory(
          inMemoryChatHomeService,
          chatRoomId -> 0,
          clock,
          8);

      @Override
      public ChatHome getChatHome()
      {
        return simpleChatHome;
      }

      @Override
      public ChatRoomFactory getChatRoomFactory()
      {
        return chatRoomFactory;
      }
    };
  }
}
