package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Slf4j
public class InMemoryChatHomeService implements ChatHomeService<InMemoryChatRoomService>
{
  private final Map<UUID, ChatRoom>[] chatrooms;


  public InMemoryChatHomeService(int numShards, Flux<ChatRoom> chatroomFlux)
  {
    log.debug("Creating InMemoryChatHomeService");
    this.chatrooms = new Map[numShards];
    for (int shard = 0; shard < numShards; shard++)
        chatrooms[shard] = new HashMap<>();
    chatroomFlux
        .toStream()
        .forEach(chatroom -> chatrooms[chatroom.getShard()].put(chatroom.getId(), chatroom));
  }

  @Override
  public Mono<ChatRoom> putChatRoom(ChatRoom chatRoom)
  {
    chatrooms[chatRoom.getShard()].put(chatRoom.getId(), chatRoom);
    return Mono.just(chatRoom);
  }

  @Override
  public Mono<ChatRoom> getChatRoom(int shard, UUID id)
  {
    return Mono.justOrEmpty(chatrooms[shard].get(id));
  }

  @Override
  public Flux<ChatRoom> getChatRooms(int shard)
  {
    return Flux.fromStream(chatrooms[shard].values().stream());
  }
}
