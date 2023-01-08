package de.juplo.kafka.chat.backend.domain;

import lombok.Getter;

import java.util.UUID;


public class UnknownChatroomException extends RuntimeException
{
  @Getter
  private final UUID chatroomId;

  public UnknownChatroomException(UUID chatroomId)
  {
    super("Chatroom does not exist: " + chatroomId);
    this.chatroomId = chatroomId;
  }
}
