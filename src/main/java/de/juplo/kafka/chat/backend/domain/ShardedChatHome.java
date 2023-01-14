package de.juplo.kafka.chat.backend.domain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
public class ShardedChatHome implements ChatHome
{
  private final ChatHome[] chatHomes;
  private final Set<Integer> ownedShards;
  private final ShardingStrategy shardingStrategy;


  public  ShardedChatHome(
      ChatHome[] chatHomes,
      ShardingStrategy shardingStrategy)
  {
    this.chatHomes = chatHomes;
    this.shardingStrategy = shardingStrategy;
    this.ownedShards = new HashSet<>();
    for (int shard = 0; shard < chatHomes.length; shard++)
      if(chatHomes[shard] != null)
        this.ownedShards.add(shard);
    log.info(
        "Created ShardedChatHome for shards: {}",
        ownedShards
            .stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", ")));
  }


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
        .fromIterable(ownedShards)
        .flatMap(shard -> chatHomes[shard].getChatRooms());
  }


  private int selectShard(UUID chatroomId)
  {
    return shardingStrategy.selectShard(chatroomId);
  }
}
