package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.Chatroom;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatroomInfo
{
  private UUID id;
  private String name;


  public static ChatroomInfo from(Chatroom chatroom)
  {
    ChatroomInfo info = new ChatroomInfo();
    info.id = chatroom.getId();
    info.name = chatroom.getName();
    return info;
  }
}
