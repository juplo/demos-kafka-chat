package de.juplo.kafka.chat.backend.domain;

import de.juplo.kafka.chat.backend.domain.exceptions.LoadInProgressException;
import de.juplo.kafka.chat.backend.domain.exceptions.UnknownChatroomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

import static pl.rzrz.assertj.reactor.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
public abstract class ChatHomeTest
{
  @Autowired
  ChatHome chatHome;


  @Test
  @DisplayName("Assert chatroom is delivered, if it exists")
  void testGetExistingChatroom()
  {
    // Given
    UUID chatRoomId = UUID.fromString("5c73531c-6fc4-426c-adcb-afc5c140a0f7");

    // When
    Mono<ChatRoomData> mono = Mono
        .defer(() -> chatHome.getChatRoomData(chatRoomId))
        .log("testGetExistingChatroom")
        .retryWhen(Retry
            .backoff(5, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof LoadInProgressException));

    // Then
    assertThat(mono).emitsCount(1);
  }

  @Test
  @DisplayName("Assert UnknownChatroomException is thrown, if chatroom does not exist")
  void testGetNonExistentChatroom()
  {
    // Given
    UUID chatRoomId = UUID.fromString("7f59ec77-832e-4a17-8d22-55ef46242c17");

    // When
    Mono<ChatRoomData> mono = Mono
        .defer(() -> chatHome.getChatRoomData(chatRoomId))
        .log("testGetNonExistentChatroom")
        .retryWhen(Retry
            .backoff(5, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof LoadInProgressException));

    // Then
    assertThat(mono).sendsError(e ->
    {
      assertThat(e).isInstanceOf(UnknownChatroomException.class);
      UnknownChatroomException unknownChatroomException = (UnknownChatroomException) e;
      assertThat(unknownChatroomException.getChatroomId()).isEqualTo(chatRoomId);
    });
  }
}
