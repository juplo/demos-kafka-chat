package de.juplo.kafka.chat.backend.implementation.kafka;

public interface Channel extends Runnable
{
  ChannelState getChannelState();
}
