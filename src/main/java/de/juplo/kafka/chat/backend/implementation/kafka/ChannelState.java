package de.juplo.kafka.chat.backend.implementation.kafka;

public enum ChannelState
{
  STARTING,
  LOAD_IN_PROGRESS,
  READY,
  SHUTTING_DOWN
}
