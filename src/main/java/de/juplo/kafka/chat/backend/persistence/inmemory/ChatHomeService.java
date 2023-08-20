package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface ChatHomeService
{
  Mono<ChatRoom> getChatRoom(int shard, UUID id);
  Flux<ChatRoom> getChatRooms(int shard);
}
