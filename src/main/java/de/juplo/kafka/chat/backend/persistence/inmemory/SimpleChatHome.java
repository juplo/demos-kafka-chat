package de.juplo.kafka.chat.backend.domain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@Slf4j
public class SimpleChatHome implements ChatHome
{
  private final ChatHomeService service;
  private final int shard;


  public SimpleChatHome(ChatHomeService service, int shard)
  {
    log.info("Created SimpleChatHome for shard {}", shard);
    this.service = service;
    this.shard = shard;
  }

  public SimpleChatHome(ChatHomeService service)
  {
    this(service, 0);
  }


  @Override
  public Mono<ChatRoom> getChatRoom(UUID id)
  {
    return service
        .getChatRoom(shard, id)
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(id)));
  }

  @Override
  public Flux<ChatRoom> getChatRooms()
  {
    return service.getChatRooms(shard);
  }
}
