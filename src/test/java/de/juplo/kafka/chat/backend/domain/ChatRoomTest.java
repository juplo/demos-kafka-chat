package de.juplo.kafka.chat.backend.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static pl.rzrz.assertj.reactor.Assertions.assertThat;


public class ChatRoomTest
{
  @Test
  @DisplayName("Assert, that Mono emits expected message, if it exists")
  void testGetExistingMessage()
  {
    // Given
    String user = "foo";
    Long messageId = 1l;
    ChatRoomService chatRoomService = mock(ChatRoomService.class);
    ChatRoom chatRoom = new ChatRoom(UUID.randomUUID(), "Foo", Clock.systemDefaultZone(), chatRoomService, 8);
    Message.MessageKey key = Message.MessageKey.of(user, messageId);
    LocalDateTime timestamp = LocalDateTime.now();
    Message message = new Message(key, 0l, timestamp, "Bar");
    when(chatRoomService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.just(message));

    // When
    Mono<Message> mono = chatRoom.getMessage(user, messageId);

    // Then
    assertThat(mono).emitsExactly(message);
  }

  @Test
  @DisplayName("Assert, that Mono if empty, if message does not exists")
  void testGetNonExistentMessage()
  {
    // Given
    String user = "foo";
    Long messageId = 1l;
    ChatRoomService chatRoomService = mock(ChatRoomService.class);
    ChatRoom chatRoom = new ChatRoom(UUID.randomUUID(), "Foo", Clock.systemDefaultZone(), chatRoomService, 8);
    when(chatRoomService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.empty());

    // When
    Mono<Message> mono = chatRoom.getMessage(user, messageId);

    // Then
    assertThat(mono).emitsCount(0);
  }

  @Test
  @DisplayName("Assert, that Mono emits expected message, if a new message is added")
  void testAddNewMessage()
  {
    // Given
    String user = "foo";
    Long messageId = 1l;
    ChatRoomService chatRoomService = mock(ChatRoomService.class);
    ChatRoom chatRoom = new ChatRoom(UUID.randomUUID(), "Foo", Clock.systemDefaultZone(), chatRoomService, 8);
    Message.MessageKey key = Message.MessageKey.of(user, messageId);
    Clock now = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    LocalDateTime timestamp = LocalDateTime.now(now);
    String messageText = "Bar";
    Message message = new Message(key, 0l, timestamp, messageText);
    when(chatRoomService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.empty());
    when(chatRoomService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class))).thenReturn(message);

    // When
    Mono<Message> mono = chatRoom.addMessage(messageId, user, messageText);

    // Then
    assertThat(mono).emitsExactly(message);
  }

  @Test
  @DisplayName("Assert, that Mono emits expected message, if an unchanged message is added")
  void testAddUnchangedMessage()
  {
    // Given
    String user = "foo";
    Long messageId = 1l;
    ChatRoomService chatRoomService = mock(ChatRoomService.class);
    ChatRoom chatRoom = new ChatRoom(UUID.randomUUID(), "Foo", Clock.systemDefaultZone(), chatRoomService, 8);
    Message.MessageKey key = Message.MessageKey.of(user, messageId);
    Clock now = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    LocalDateTime timestamp = LocalDateTime.now(now);
    String messageText = "Bar";
    Message message = new Message(key, 0l, timestamp, messageText);
    when(chatRoomService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.just(message));
    when(chatRoomService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class))).thenReturn(message);

    // When
    Mono<Message> mono = chatRoom.addMessage(messageId, user, messageText);

    // Then
    assertThat(mono).emitsExactly(message);
  }

  @Test
  @DisplayName("Assert, that Mono sends an error, if a message is added again with mutated text")
  void testAddMutatedMessage()
  {
    // Given
    String user = "foo";
    Long messageId = 1l;
    ChatRoomService chatRoomService = mock(ChatRoomService.class);
    ChatRoom chatRoom = new ChatRoom(UUID.randomUUID(), "Foo", Clock.systemDefaultZone(), chatRoomService, 8);
    Message.MessageKey key = Message.MessageKey.of(user, messageId);
    Clock now = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    LocalDateTime timestamp = LocalDateTime.now(now);
    String messageText = "Bar";
    String mutatedText = "Boom!";
    Message message = new Message(key, 0l, timestamp, messageText);
    when(chatRoomService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.just(message));
    when(chatRoomService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class))).thenReturn(message);

    // When
    Mono<Message> mono = chatRoom.addMessage(messageId, user, mutatedText);

    // Then
    assertThat(mono).sendsError();
  }
}
