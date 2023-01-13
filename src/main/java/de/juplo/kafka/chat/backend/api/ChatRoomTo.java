package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatRoomTo
{
  private UUID id;
  private String name;


  public static ChatRoomTo from(ChatRoom chatroom)
  {
    ChatRoomTo to = new ChatRoomTo();
    to.id = chatroom.getId();
    to.name = chatroom.getName();
    return to;
  }
}
