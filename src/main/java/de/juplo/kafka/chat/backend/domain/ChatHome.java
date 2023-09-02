package de.juplo.kafka.chat.backend.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface ChatHome
{
  Mono<ChatRoomInfo> createChatRoom(UUID id, String name);

  Mono<ChatRoom> getChatRoom(UUID id);

  Flux<ChatRoom> getChatRooms();
}
