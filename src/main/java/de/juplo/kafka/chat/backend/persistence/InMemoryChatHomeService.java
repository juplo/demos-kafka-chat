package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.UUID;


@RequiredArgsConstructor
public class InMemoryChatHomeService implements ChatHomeService<InMemoryChatroomService>
{
  private final int bufferSize;


  @Override
  public ChatRoom createChatroom(UUID id, String name)
  {
    InMemoryChatroomService chatroomService =
        new InMemoryChatroomService(new LinkedHashMap<>());
    return new ChatRoom(id, name, chatroomService, bufferSize);
  }

  public ChatRoom restoreChatroom(
      UUID id,
      String name,
      InMemoryChatroomService chatroomService)
  {
    return new ChatRoom(id, name, chatroomService, bufferSize);
  }
}
