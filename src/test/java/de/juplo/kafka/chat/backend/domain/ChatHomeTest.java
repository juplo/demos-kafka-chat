package de.juplo.kafka.chat.backend.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static pl.rzrz.assertj.reactor.Assertions.assertThat;


public class ChatHomeTest
{
  @Test
  @DisplayName("Assert chatroom is delivered, if it exists")
  void testGetExistingChatroom()
  {
    // Given
    ChatHomeService chatHomeService = mock(ChatHomeService.class);
    ChatRoomService chatRoomService = mock(ChatRoomService.class);
    UUID chatroomId = UUID.randomUUID();
    ChatRoom chatRoom = new ChatRoom(chatroomId, "Foo", Clock.systemDefaultZone(), chatRoomService, 8);
    ChatHome chatHome = new ChatHome(chatHomeService, Flux.just(chatRoom));

    // When
    Mono<ChatRoom> mono = chatHome.getChatroom(chatroomId);

    // Then
    assertThat(mono).emitsExactly(chatRoom);
  }

  @Test
  @DisplayName("Assert UnknownChatroomException is thrown, if chatroom does not exist")
  void testGetNonExistentChatroom()
  {
    // Given
    ChatHomeService chatHomeService = mock(ChatHomeService.class);
    ChatHome chatHome = new ChatHome(chatHomeService, Flux.empty());

    // When
    Mono<ChatRoom> mono = chatHome.getChatroom(UUID.randomUUID());

    // Then
    assertThat(mono).sendsError();
  }
}
