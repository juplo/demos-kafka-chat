package de.juplo.kafka.chat.backend.domain.exceptions;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import lombok.Getter;


public class ChatRoomInactiveException extends IllegalStateException
{
  @Getter
  private final ChatRoomInfo chatRoomInfo;


  public ChatRoomInactiveException(ChatRoomInfo chatRoomInfo)
  {
    super("Chat-Room " + chatRoomInfo + " is currently inactive.");
    this.chatRoomInfo = chatRoomInfo;
  }
}
