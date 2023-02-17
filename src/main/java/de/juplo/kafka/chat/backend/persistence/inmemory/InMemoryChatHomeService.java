package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@Slf4j
public class InMemoryChatHomeService implements ChatHomeService
{
  private final Map<UUID, ChatRoom>[] chatrooms;


  public InMemoryChatHomeService(
      int numShards,
      int[] ownedShards,
      Flux<ChatRoom> chatroomFlux)
  {
    log.debug("Creating InMemoryChatHomeService");
    this.chatrooms = new Map[numShards];
    Set<Integer> owned = Arrays
        .stream(ownedShards)
        .collect(
            () -> new HashSet<>(),
            (set, i) -> set.add(i),
            (a, b) -> a.addAll(b));
    for (int shard = 0; shard < numShards; shard++)
    {
      chatrooms[shard] = owned.contains(shard)
          ? new HashMap<>()
          : null;
    }
    chatroomFlux
        .filter(chatRoom ->
        {
          if (owned.contains(chatRoom.getShard()))
          {
            return true;
          }
          else
          {
            log.info("Ignoring not owned chat-room {}", chatRoom);
            return false;
          }
        })
        .toStream()
        .forEach(chatroom -> chatrooms[chatroom.getShard()].put(chatroom.getId(), chatroom));
  }

  public void putChatRoom(ChatRoom chatRoom)
  {
    chatrooms[chatRoom.getShard()].put(chatRoom.getId(), chatRoom);
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
