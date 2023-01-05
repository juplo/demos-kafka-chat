package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.Chatroom;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatroomTo
{
  private UUID id;
  private String name;


  public static ChatroomTo from(Chatroom chatroom)
  {
    ChatroomTo info = new ChatroomTo();
    info.id = chatroom.getId();
    info.name = chatroom.getName();
    return info;
  }
}
