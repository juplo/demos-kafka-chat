package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.domain.*;
import de.juplo.kafka.chat.backend.domain.exceptions.ShardNotOwnedException;
import de.juplo.kafka.chat.backend.domain.exceptions.UnknownChatroomException;
import de.juplo.kafka.chat.backend.implementation.ShardingStrategy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
public class ShardedChatHomeService implements ChatHomeService
{
  private final SimpleChatHomeService[] chatHomes;
  private final Set<Integer> ownedShards;
  private final ShardingStrategy shardingStrategy;


  public ShardedChatHomeService(
      SimpleChatHomeService[] chatHomes,
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
  public Mono<ChatRoomInfo> createChatRoom(UUID id, String name)
  {
    int shard = shardingStrategy.selectShard(id);
    return chatHomes[shard] == null
        ? Mono.error(new ShardNotOwnedException(shard))
        : chatHomes[shard].createChatRoom(id, name);
  }

  @Override
  public Mono<ChatRoomInfo> getChatRoomInfo(UUID id)
  {
    int shard = selectShard(id);
    return chatHomes[shard] == null
        ? Mono.error(new ShardNotOwnedException(shard))
        : chatHomes[shard]
            .getChatRoomInfo(id)
            .onErrorMap(throwable -> throwable instanceof UnknownChatroomException
            ? new UnknownChatroomException(
                id,
                shard,
                ownedShards.stream().mapToInt(i -> i.intValue()).toArray())
            : throwable);
  }

  @Override
  public Flux<ChatRoomInfo> getChatRoomInfo()
  {
    return Flux
        .fromIterable(ownedShards)
        .flatMap(shard -> chatHomes[shard].getChatRoomInfo());
  }

  @Override
  public Mono<ChatRoomData> getChatRoomData(UUID id)
  {
    int shard = selectShard(id);
    return chatHomes[shard] == null
        ? Mono.error(new ShardNotOwnedException(shard))
        : chatHomes[shard]
            .getChatRoomData(id)
            .onErrorMap(throwable -> throwable instanceof UnknownChatroomException
                ? new UnknownChatroomException(
                id,
                shard,
                ownedShards.stream().mapToInt(i -> i.intValue()).toArray())
                : throwable);
  }

  private int selectShard(UUID chatroomId)
  {
    return shardingStrategy.selectShard(chatroomId);
  }
}
