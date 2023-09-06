package de.juplo.kafka.chat.backend.domain;

import de.juplo.kafka.chat.backend.domain.exceptions.LoadInProgressException;
import de.juplo.kafka.chat.backend.domain.exceptions.ShardNotOwnedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

import static pl.rzrz.assertj.reactor.Assertions.assertThat;


public abstract class ChatHomeServiceWithShardsTest extends ChatHomeServiceTest
{
  public static final int NUM_SHARDS = 10;
  public static final int OWNED_SHARD = 2;
  public static final int NOT_OWNED_SHARD = 0;


  @Test
  @DisplayName("Assert ShardNotOwnedException is thrown, if the shard for the chatroom is not owned")
  void testGetChatroomForNotOwnedShard()
  {
    // Given
    UUID chatRoomId = UUID.fromString("4e7246a6-29ae-43ea-b56f-669c3481ac19");

    // When
    Mono<ChatRoomData> mono = Mono
        .defer(() -> chatHomeService.getChatRoomData(chatRoomId))
        .log("testGetChatroomForNotOwnedShard")
        .retryWhen(Retry
            .backoff(5, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof LoadInProgressException));

    // Then
    assertThat(mono).sendsError(e ->
    {
      assertThat(e).isInstanceOf(ShardNotOwnedException.class);
      ShardNotOwnedException shardNotOwnedException = (ShardNotOwnedException) e;
      assertThat(shardNotOwnedException.getShard()).isEqualTo(NOT_OWNED_SHARD);
    });
  }
}
