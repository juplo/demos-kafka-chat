package de.juplo.kafka.chat.backend.persistence.kafka;


public class LoadInProgressException extends IllegalStateException
{
  public LoadInProgressException()
  {
    super("Load in progress...");
  }
}
