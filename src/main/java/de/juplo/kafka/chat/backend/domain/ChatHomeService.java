package de.juplo.kafka.chat.backend.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface ChatHomeService
{
  Mono<ChatRoomInfo> createChatRoom(UUID id, String name);

  Mono<ChatRoomInfo> getChatRoomInfo(UUID id);

  Flux<ChatRoomInfo> getChatRoomInfo();

  Mono<ChatRoomData> getChatRoomData(UUID id);

  Mono<String[]> getShardOwners();
}
