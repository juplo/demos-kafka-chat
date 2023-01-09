package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import reactor.core.publisher.Flux;


public interface StorageStrategy
{
  void write(Flux<ChatRoom> chatroomFlux);
  Flux<ChatRoom> read();
}
