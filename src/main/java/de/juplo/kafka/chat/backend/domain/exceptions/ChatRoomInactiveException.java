package de.juplo.kafka.chat.backend.domain.exceptions;

import lombok.Getter;

import java.util.UUID;


public class ChatRoomInactiveException extends IllegalStateException
{
  @Getter
  private final UUID chatRoomId;


  public ChatRoomInactiveException(UUID chatRoomId)
  {
    super("Chat-Room " + chatRoomId + " is currently inactive.");
    this.chatRoomId = chatRoomId;
  }
}
