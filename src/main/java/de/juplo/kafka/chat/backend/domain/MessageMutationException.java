package de.juplo.kafka.chat.backend.domain;

import lombok.Getter;


public class MessageMutationException extends RuntimeException
{
  @Getter
  private final Message mutated;
  @Getter
  private final Message existing;

  public MessageMutationException(Message mutated, Message existing)
  {
    super("Messages are imutable!");
    this.mutated = mutated;
    this.existing = existing;
  }
}
