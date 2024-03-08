package de.juplo.kafka.chat.backend.domain;

import org.junit.jupiter.api.BeforeEach;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.LinkedList;
import java.util.List;

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
        now,
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
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.just(someMessage()));

    // When
    Mono<Message> mono = chatRoomData.getMessage(user, messageId);

    // Then
    assertThat(mono).emitsExactly(someMessage());
  }

  @Test
  @DisplayName("Assert, that Mono is empty, if message does not exists")
  void testGetNonExistentMessage()
  {
    // Given
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.empty());

    // When
    Mono<Message> mono = chatRoomData.getMessage(user, messageId);

    // Then
    assertThat(mono).emitsCount(0);
  }

  @Test
  @DisplayName("Assert, that Mono emits the persisted message, if a new message is added")
  void testAddNewMessageEmitsPersistedMessage()
  {
    // Given
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.empty());
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class)))
        .thenReturn(Mono.just(someMessage()));

    // When
    Mono<Message> mono = chatRoomData.addMessage(messageId, user, "Some Text");

    // Then
    assertThat(mono).emitsExactly(someMessage());
  }

  @Test
  @DisplayName("Assert, that ChatMessageService.persistMessage() is called correctly, if a new message is added")
  void testAddNewMessageTriggersPersistence()
  {
    // Given
    String messageText = "Bar";
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.empty());
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class)))
        .thenReturn(Mono.just(someMessage()));

    // When
    chatRoomData
        .addMessage(messageId, user, messageText)
        .block();

    // Then
    verify(chatMessageService, times(1)).persistMessage(eq(key), eq(timestamp), eq(messageText));
  }

  @Test
  @DisplayName("Assert, that Mono emits the already persisted message, if an unchanged message is added")
  void testAddUnchangedMessageEmitsAlreadyPersistedMessage()
  {
    // Given
    String messageText = "Bar";
    Message existingMessage = new Message(key, 0l, timestamp, messageText);
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.just(existingMessage));

    // When
    Mono<Message> mono = chatRoomData.addMessage(messageId, user, messageText);

    // Then
    assertThat(mono).emitsExactly(existingMessage);
  }

  @Test
  @DisplayName("Assert, that ChatMessageService.persistMessage() is not called, if an unchanged message is added")
  void testAddUnchangedMessageDoesNotTriggerPersistence()
  {
    // Given
    String messageText = "Bar";
    Message existingMessage = new Message(key, 0l, timestamp, messageText);
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.just(existingMessage));

    // When
    chatRoomData
        .addMessage(messageId, user, messageText)
        .block();

    // Then
    verify(chatMessageService, never()).persistMessage(any(), any(), any());
  }

  @Test
  @DisplayName("Assert, that Mono sends an error, if a message is added again with mutated text")
  void testAddMutatedMessageSendsError()
  {
    // Given
    String messageText = "Bar";
    String mutatedText = "Boom!";
    Message existingMessage = new Message(key, 0l, timestamp, messageText);
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.just(existingMessage));

    // When
    Mono<Message> mono = chatRoomData.addMessage(messageId, user, mutatedText);

    // Then
    assertThat(mono).sendsError();
  }

  @Test
  @DisplayName("Assert, that ChatMessageService.persistMessage() is not called, if a message is added again with mutated text")
  void testAddMutatedMessageDoesNotTriggerPersistence()
  {
    // Given
    String messageText = "Bar";
    String mutatedText = "Boom!";
    Message existingMessage = new Message(key, 0l, timestamp, messageText);
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.just(existingMessage));

    // When
    chatRoomData
        .addMessage(messageId, user, mutatedText)
        .onErrorResume((throwable) -> Mono.empty())
        .block();

    // Then
    verify(chatMessageService, never()).persistMessage(any(), any(), any());
  }

  @Test
  @DisplayName("Assert, that a listener receives a message, that was added after the listening had started")
  void testListenerReceivesMessageAddedAfterListeningStarts()
  {
    // Given
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.empty());
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class)))
        .thenReturn(Mono.just(someMessage()));

    // When
    List<Message> receivedMessages = new LinkedList<>();
    chatRoomData
        .listen()
        .subscribe(receivedMessage -> receivedMessages.add(receivedMessage));
    Message sentMessage = chatRoomData
        .addMessage(messageId, user, "Some Text")
        .block();

    // Then
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(() -> assertThat(receivedMessages).contains(sentMessage));
  }

  @Test
  @DisplayName("Assert, that a listener receives a message, that was added before the listening had started")
  void testListenerReceivesMessageAddedBeforeListeningStarts()
  {
    // Given
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.empty());
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class)))
        .thenReturn(Mono.just(someMessage()));

    // When
    Message sentMessage = chatRoomData
        .addMessage(messageId, user, "Some Text")
        .block();
    List<Message> receivedMessages = new LinkedList<>();
    chatRoomData
        .listen()
        .subscribe(receivedMessage -> receivedMessages.add(receivedMessage));

    // Then
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(() -> assertThat(receivedMessages).contains(sentMessage));
  }

  @Test
  @DisplayName("Assert, that a listener receives several messages, that were added before and after the listening had started, in correct order")
  void testListenerReceivesMessagesFromBeforeAndAfterListeningHadStartedInCorrectOrder()
  {
    // Given
    Message message1 = new Message(key, 1l, timestamp, "#1");
    Message message2 = new Message(key, 2l, timestamp, "#2");
    Message message3 = new Message(key, 3l, timestamp, "#3");
    Message message4 = new Message(key, 4l, timestamp, "#4");
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.empty());
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class)))
        .thenReturn(Mono.just(message1))
        .thenReturn(Mono.just(message2))
        .thenReturn(Mono.just(message3))
        .thenReturn(Mono.just(message4));

    // When
    Message[] sentMessages = new Message[4];
    sentMessages[0] = chatRoomData.addMessage(messageId, user, "Some Text").block();
    sentMessages[1] = chatRoomData.addMessage(messageId, user, "Some Text").block();
    List<Message> receivedMessages = new LinkedList<>();
    chatRoomData
        .listen()
        .subscribe(receivedMessage -> receivedMessages.add(receivedMessage));
    sentMessages[2] = chatRoomData.addMessage(messageId, user, "Some Text").block();
    sentMessages[3] = chatRoomData.addMessage(messageId, user, "Some Text").block();

    // Then
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(() -> assertThat(receivedMessages).contains(sentMessages));
  }


  /**
   * This message is used, when methods of {@link ChatMessageService} are mocked,
   * that return a {@link Message}.
   * The contents of the message are set to arbitrary values, in order to underline
   * the fact, that the test can only assert, that the message that was returned
   * by {@link ChatMessageService} is handed on by {@link ChatRoomData} correctly.
   * @return a message.
   */
  private Message someMessage()
  {
    return new Message(
        Message.MessageKey.of("FOO", 666l),
        666l,
        LocalDateTime.of(2024, 3, 8, 12, 13, 00),
        "Just some message...");
  }
}
