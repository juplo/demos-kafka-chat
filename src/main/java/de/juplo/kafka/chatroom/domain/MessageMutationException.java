package de.juplo.kafka.chatroom.domain;

import lombok.Getter;


public class MessageMutationException extends RuntimeException
{
  @Getter
  private final Message toAdd;
  @Getter
  private final Message existing;

  public MessageMutationException(Message toAdd, Message existing)
  {
    super("Messages are imutable!");
    this.toAdd = toAdd;
    this.existing = existing;
  }
}
