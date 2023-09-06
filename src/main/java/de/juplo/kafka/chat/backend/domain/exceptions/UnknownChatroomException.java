package de.juplo.kafka.chat.backend.domain.exceptions;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


public class UnknownChatroomException extends IllegalStateException
{
  @Getter
  private final UUID chatroomId;
  @Getter
  private final Optional<Integer> shard;
  @Getter
  private final Optional<int[]> ownedShards;

  public UnknownChatroomException(UUID chatroomId)
  {
    super("Chatroom does not exist: " + chatroomId);
    this.chatroomId = chatroomId;
    this.shard = Optional.empty();
    this.ownedShards = Optional.empty();
  }

  public UnknownChatroomException(UUID chatroomId, int shard, int[] ownedShards)
  {
    super(
        "Chatroom does not exist (here): " +
        chatroomId +
        " shard=" +
        shard +
        ", owned=" +
        Arrays
            .stream(ownedShards)
            .mapToObj(ownedShard -> Integer.toString(ownedShard))
            .collect(Collectors.joining(",")));
    this.chatroomId = chatroomId;
    this.shard = Optional.of(shard);
    this.ownedShards = Optional.of(ownedShards);
  }
}
