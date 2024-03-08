package de.juplo.kafka.chat.backend.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.*;
import static pl.rzrz.assertj.reactor.Assertions.assertThat;


public class ChatRoomDataTest
{
  Clock now;
  ChatMessageService chatMessageService;
  ChatRoomData chatRoomData;

  String user;
  Long messageId;
  Message.MessageKey key;
  LocalDateTime timestamp;


  @BeforeEach
  public void setUp()
  {
    now = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    chatMessageService = mock(ChatMessageService.class);
    chatRoomData = new ChatRoomData(
        Clock.systemDefaultZone(),
        chatMessageService,
        8);

    user = "foo";
    messageId = 1l;
    key = Message.MessageKey.of(user, messageId);
    timestamp = LocalDateTime.now(now);
  }


  @Test
  @DisplayName("Assert, that Mono emits expected message, if it exists")
  void testGetExistingMessage()
  {
    // Given
    Message message = new Message(key, 0l, timestamp, "Bar");
    when(chatMessageService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.just(message));

    // When
    Mono<Message> mono = chatRoomData.getMessage(user, messageId);

    // Then
    assertThat(mono).emitsExactly(message);
  }

  @Test
  @DisplayName("Assert, that Mono is empty, if message does not exists")
  void testGetNonExistentMessage()
  {
    // Given
    when(chatMessageService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.empty());

    // When
    Mono<Message> mono = chatRoomData.getMessage(user, messageId);

    // Then
    assertThat(mono).emitsCount(0);
  }

  @Test
  @DisplayName("Assert, that Mono emits expected message, if a new message is added")
  void testAddNewMessage()
  {
    // Given
    String messageText = "Bar";
    Message message = new Message(key, 0l, timestamp, messageText);
    when(chatMessageService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.empty());
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class))).thenReturn(Mono.just(message));

    // When
    Mono<Message> mono = chatRoomData.addMessage(messageId, user, messageText);

    // Then
    assertThat(mono).emitsExactly(message);
  }

  @Test
  @DisplayName("Assert, that Mono emits expected message, if an unchanged message is added")
  void testAddUnchangedMessage()
  {
    // Given
    String messageText = "Bar";
    Message message = new Message(key, 0l, timestamp, messageText);
    when(chatMessageService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.just(message));
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class))).thenReturn(Mono.just(message));

    // When
    Mono<Message> mono = chatRoomData.addMessage(messageId, user, messageText);

    // Then
    assertThat(mono).emitsExactly(message);
  }

  @Test
  @DisplayName("Assert, that Mono sends an error, if a message is added again with mutated text")
  void testAddMutatedMessage()
  {
    // Given
    String messageText = "Bar";
    String mutatedText = "Boom!";
    Message message = new Message(key, 0l, timestamp, messageText);
    when(chatMessageService.getMessage(any(Message.MessageKey.class))).thenReturn(Mono.just(message));
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class))).thenReturn(Mono.just(message));

    // When
    Mono<Message> mono = chatRoomData.addMessage(messageId, user, mutatedText);

    // Then
    assertThat(mono).sendsError();
  }
}
