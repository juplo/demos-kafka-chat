package de.juplo.kafka.chat.backend.implementation.kafka;

import org.apache.kafka.clients.consumer.Consumer;


public interface WorkAssignor
{
  void assignWork(Consumer<?, ?> consumer);
}
