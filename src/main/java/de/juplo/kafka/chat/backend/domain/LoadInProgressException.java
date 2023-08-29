package de.juplo.kafka.chat.backend.domain;


public class LoadInProgressException extends IllegalStateException
{
  public LoadInProgressException()
  {
    super("Load in progress...");
  }
}
