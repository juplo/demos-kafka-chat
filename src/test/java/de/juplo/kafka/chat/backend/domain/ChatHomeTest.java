package de.juplo.kafka.chat.backend.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.rzrz.assertj.reactor.Assertions.assertThat;


public class ChatHomeTest
{
  @Test
  @DisplayName("Assert chatroom is delivered, if it exists")
  void testGetExistingChatroom()
  {
    // Given
    ChatHomeService chatHomeService = mock(ChatHomeService.class);
    ChatRoom chatRoom = new ChatRoom(
        UUID.randomUUID(),
        "Foo",
        Clock.systemDefaultZone(),
        mock(ChatRoomService.class),
        8);
    when(chatHomeService.getChatRoom(any(UUID.class))).thenReturn(Mono.just(chatRoom));
    ChatHome chatHome = new ChatHome(chatHomeService);

    // When
    Mono<ChatRoom> mono = chatHome.getChatRoom(chatRoom.getId());

    // Then
    assertThat(mono).emitsExactly(chatRoom);
  }

  @Test
  @DisplayName("Assert UnknownChatroomException is thrown, if chatroom does not exist")
  void testGetNonExistentChatroom()
  {
    // Given
    ChatHomeService chatHomeService = mock(ChatHomeService.class);
    when(chatHomeService.getChatRoom(any(UUID.class))).thenReturn(Mono.empty());
    ChatHome chatHome = new ChatHome(chatHomeService);

    // When
    Mono<ChatRoom> mono = chatHome.getChatRoom(UUID.randomUUID());

    // Then
    assertThat(mono).sendsError();
  }
}
