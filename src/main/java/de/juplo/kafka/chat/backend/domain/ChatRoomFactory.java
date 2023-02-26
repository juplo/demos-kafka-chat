package de.juplo.kafka.chat.backend.domain;

import reactor.core.publisher.Mono;

import java.util.UUID;


public interface ChatRoomFactory
{
  Mono<ChatRoom> createChatRoom(UUID id, String name);
}
