package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatRoomInfoTo
{
  private UUID id;
  private String name;
  private int shard;


  public static ChatRoomInfoTo from(ChatRoomInfo info)
  {
    ChatRoomInfoTo to = new ChatRoomInfoTo();
    to.id = info.getId();
    to.name = info.getName();
    to.shard = info.getShard();
    return to;
  }
}
