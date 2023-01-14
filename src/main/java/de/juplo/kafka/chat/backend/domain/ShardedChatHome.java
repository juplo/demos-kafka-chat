package de.juplo.kafka.chat.backend.domain;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RequiredArgsConstructor
public class ShardedChatHome implements ChatHome
{
  private final ChatHome[] chatHomes;
  private final ShardingStrategy selectionStrategy;


  @Override
  public Mono<ChatRoom> putChatRoom(ChatRoom chatRoom)
  {
    return chatHomes[selectShard(chatRoom.getId())].putChatRoom(chatRoom);
  }

  @Override
  public Mono<ChatRoom> getChatRoom(UUID id)
  {
    return chatHomes[selectShard(id)].getChatRoom(id);
  }

  @Override
  public Flux<ChatRoom> getChatRooms()
  {
    return Flux
        .fromArray(chatHomes)
        .flatMap(chatHome -> chatHome.getChatRooms());
  }


  private int selectShard(UUID chatroomId)
  {
    return selectionStrategy.selectShard(chatroomId);
  }
}
