package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.Chatroom;
import de.juplo.kafka.chat.backend.domain.ChatroomFactory;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.UUID;


@RequiredArgsConstructor
public class InMemoryChatroomFactory implements ChatroomFactory<InMemoryChatroomService>
{
  private final int bufferSize;


  @Override
  public Chatroom createChatroom(UUID id, String name)
  {
    InMemoryChatroomService chatroomService =
        new InMemoryChatroomService(new LinkedHashMap<>());
    return new Chatroom(id, name, chatroomService, bufferSize);
  }

  public Chatroom restoreChatroom(
      UUID id,
      String name,
      InMemoryChatroomService chatroomService)
  {
    return new Chatroom(id, name, chatroomService, bufferSize);
  }
}
