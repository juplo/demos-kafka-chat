package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.Chatroom;
import de.juplo.kafka.chat.backend.domain.ChatroomFactory;
import de.juplo.kafka.chat.backend.domain.PersistenceStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InMemoryChatroomFactory implements ChatroomFactory
{
  private final PersistenceStrategy persistenceStrategy;


  @Override
  public Chatroom createChatroom(UUID id, String name)
  {
    return new Chatroom(id, name, persistenceStrategy);
  }
}
