package de.juplo.kafka.chat.backend.domain.exceptions;


public class LoadInProgressException extends IllegalStateException
{
  public LoadInProgressException()
  {
    super("Load in progress...");
  }
}
