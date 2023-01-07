package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.Chatroom;
import de.juplo.kafka.chat.backend.domain.ChatroomFactory;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.UUID;


@RequiredArgsConstructor
public class InMemoryChatroomFactory implements ChatroomFactory<InMemoryPersistenceStrategy>
{
  private final int bufferSize;


  @Override
  public Chatroom createChatroom(UUID id, String name)
  {
    InMemoryPersistenceStrategy persistenceStrategy =
        new InMemoryPersistenceStrategy(new LinkedHashMap<>());
    return new Chatroom(id, name, persistenceStrategy, bufferSize);
  }

  @Override
  public Chatroom restoreChatroom(
      UUID id,
      String name,
      InMemoryPersistenceStrategy persistenceStrategy)
  {
    return new Chatroom(id, name, persistenceStrategy, bufferSize);
  }
}
