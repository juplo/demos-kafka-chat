package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.UnknownChatroomException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@Slf4j
public class SimpleChatHome implements ChatHome
{
  private final InMemoryChatHomeService service;
  private final int shard;


  public SimpleChatHome(InMemoryChatHomeService service, int shard)
  {
    log.info("Created SimpleChatHome for shard {}", shard);
    this.service = service;
    this.shard = shard;
  }

  public SimpleChatHome(InMemoryChatHomeService service)
  {
    this(service, 0);
  }


  @Override
  public Mono<ChatRoom> getChatRoom(UUID id)
  {
    return service
        .getChatRoom(shard, id)
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(
            id,
            shard,
            service.getOwnedShards())));
  }

  @Override
  public Flux<ChatRoom> getChatRooms()
  {
    return service.getChatRooms(shard);
  }
}
