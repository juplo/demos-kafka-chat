package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.UUID;


@RequiredArgsConstructor
public class InMemoryChatHomeService implements ChatHomeService<InMemoryChatRoomService>
{
  private final int bufferSize;


  @Override
  public ChatRoom createChatroom(UUID id, String name)
  {
    InMemoryChatRoomService service =
        new InMemoryChatRoomService(new LinkedHashMap<>());
    return new ChatRoom(id, name, service, bufferSize);
  }

  public ChatRoom restoreChatroom(
      UUID id,
      String name,
      InMemoryChatRoomService service)
  {
    return new ChatRoom(id, name, service, bufferSize);
  }
}
