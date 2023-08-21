package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatRoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.rzrz.assertj.reactor.Assertions.assertThat;


public class SimpleChatHomeTest
{
  @Test
  @DisplayName("Assert chatroom is delivered, if it exists")
  void testGetExistingChatroom()
  {
    // Given
    InMemoryChatHomeService chatHomeService = mock(InMemoryChatHomeService.class);
    ChatRoom chatRoom = new ChatRoom(
        UUID.randomUUID(),
        "Foo",
        0,
        Clock.systemDefaultZone(),
        mock(ChatRoomService.class),
        8);
    when(chatHomeService.getChatRoom(anyInt(), any(UUID.class))).thenReturn(Mono.just(chatRoom));
    SimpleChatHome chatHome = new SimpleChatHome(chatHomeService);

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
    InMemoryChatHomeService chatHomeService = mock(InMemoryChatHomeService.class);
    when(chatHomeService.getChatRoom(anyInt(), any(UUID.class))).thenReturn(Mono.empty());
    SimpleChatHome chatHome = new SimpleChatHome(chatHomeService);

    // When
    Mono<ChatRoom> mono = chatHome.getChatRoom(UUID.randomUUID());

    // Then
    assertThat(mono).sendsError();
  }
}
