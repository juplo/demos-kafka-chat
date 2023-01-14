package de.juplo.kafka.chat.backend.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface ChatHome
{
  Mono<ChatRoom> putChatRoom(ChatRoom chatRoom);

  Mono<ChatRoom> getChatRoom(UUID id);

  Flux<ChatRoom> getChatRooms();
}
