package de.juplo.kafka.chat.backend.implementation.kafka;


public class ChannelNotReadyException extends IllegalStateException
{
  public final ChannelState state;

  public ChannelNotReadyException(ChannelState state)
  {
    super("Not ready! Current state: " + state);
    this.state = state;
  }
}
