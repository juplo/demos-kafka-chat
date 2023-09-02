package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.*;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;


@Slf4j
public class SimpleChatHome implements ChatHome
{
  private final Integer shard;
  private final Map<UUID, ChatRoom> chatRooms;
  private final Clock clock;
  private final int bufferSize;



  public SimpleChatHome(
      Flux<ChatRoom> chatroomFlux,
      Clock clock,
      int bufferSize)
  {
    this(null, chatroomFlux, clock, bufferSize);
  }

  public SimpleChatHome(
      Integer shard,
      Flux<ChatRoom> chatroomFlux,
      Clock clock,
      int bufferSize)
  {
    log.info("Created SimpleChatHome for shard {}", shard);
;
    this.shard = shard;
    this.chatRooms = new HashMap<>();
    chatroomFlux
        .filter(chatRoom ->
        {
          if (shard == null || chatRoom.getShard() == shard)
          {
            return true;
          }
          else
          {
            log.info(
                "SimpleChatHome for shard {} ignores not owned chat-room {}",
                shard,
                chatRoom);
            return false;
          }
        })
        .toStream()
        .forEach(chatroom -> chatRooms.put(chatroom.getId(), chatroom));
    this.clock = clock;
    this.bufferSize = bufferSize;
  }


  @Override
  public Mono<ChatRoomInfo> createChatRoom(UUID id, String name)
  {
    log.info("Creating ChatRoom with buffer-size {}", bufferSize);
    ChatRoomService service = new InMemoryChatRoomService(Flux.empty());
    ChatRoom chatRoom = new ChatRoom(id, name, shard, clock, service, bufferSize);
    chatRooms.put(id, chatRoom);
    return Mono.just(chatRoom);
  }

  @Override
  public Mono<ChatRoom> getChatRoom(UUID id)
  {
    return Mono
        .justOrEmpty(chatRooms.get(id))
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(id)));
  }

  @Override
  public Flux<ChatRoom> getChatRooms()
  {
    return Flux.fromIterable(chatRooms.values());
  }
}
