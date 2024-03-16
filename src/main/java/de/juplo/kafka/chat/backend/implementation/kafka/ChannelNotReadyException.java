package de.juplo.kafka.chat.backend.implementation.kafka;


import lombok.Getter;


public class ChannelNotReadyException extends IllegalStateException
{
  @Getter
  public final ChannelState state;

  public ChannelNotReadyException(ChannelState state)
  {
    super("Channel not ready! Current state: " + state);
    this.state = state;
  }
}
