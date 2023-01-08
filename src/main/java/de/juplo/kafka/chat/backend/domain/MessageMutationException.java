package de.juplo.kafka.chat.backend.domain;

import lombok.Getter;


public class MessageMutationException extends RuntimeException
{
  @Getter
  private final Message existing;
  @Getter
  private final String mutatedText;

  public MessageMutationException(Message existing, String mutatedText)
  {
    super("Messages are imutable!");
    this.existing = existing;
    this.mutatedText = mutatedText;
  }
}
