package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.*;
import de.juplo.kafka.chat.backend.domain.exceptions.ShardNotOwnedException;
import de.juplo.kafka.chat.backend.domain.exceptions.UnknownChatroomException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    })
@AutoConfigureWebTestClient
@Slf4j
public class ChatBackendControllerTest
{
  @Autowired
  ChatBackendProperties properties;

  @MockBean
  ChatHomeService chatHomeService;
  @MockBean
  ChatMessageService chatMessageService;

  @Test
  @DisplayName("Assert expected problem-details for unknown chatroom on GET /list/{chatroomId}")
  void testUnknownChatroomExceptionForListChatroom(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenThrow(new UnknownChatroomException(chatroomId));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/{chatroomId}/list", chatroomId)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Then
    assertProblemDetailsForUnknownChatroomException(responseSpec, chatroomId);
  }


  @Test
  @DisplayName("Assert expected problem-details for unknown chatroom on GET /get/{chatroomId}")
  void testUnknownChatroomExceptionForGetChatroom(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    when(chatHomeService.getChatRoomInfo(eq(chatroomId))).thenThrow(new UnknownChatroomException(chatroomId));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/{chatroomId}", chatroomId)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Then
    assertProblemDetailsForUnknownChatroomException(responseSpec, chatroomId);
  }

  @Test
  @DisplayName("Assert expected problem-details for unknown chatroom on PUT /put/{chatroomId}/{username}/{messageId}")
  void testUnknownChatroomExceptionForPutMessage(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String username = "foo";
    Long messageId = 66l;
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenThrow(new UnknownChatroomException(chatroomId));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .put()
        .uri(
            "/{chatroomId}/{username}/{messageId}",
            chatroomId,
            username,
            messageId)
        .bodyValue("bar")
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Then
    assertProblemDetailsForUnknownChatroomException(responseSpec, chatroomId);
  }

  @Test
  @DisplayName("Assert expected problem-details for unknown chatroom on GET /get/{chatroomId}/{username}/{messageId}")
  void testUnknownChatroomExceptionForGetMessage(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String username = "foo";
    Long messageId = 66l;
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenThrow(new UnknownChatroomException(chatroomId));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri(
            "/{chatroomId}/{username}/{messageId}",
            chatroomId,
            username,
            messageId)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Then
    assertProblemDetailsForUnknownChatroomException(responseSpec, chatroomId);
  }

  @Test
  @DisplayName("Assert expected problem-details for unknown chatroom on GET /listen/{chatroomId}")
  void testUnknownChatroomExceptionForListenChatroom(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenThrow(new UnknownChatroomException(chatroomId));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/{chatroomId}/listen", chatroomId)
        // .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON) << TODO: Does not work!
        .exchange();

    // Then
    assertProblemDetailsForUnknownChatroomException(responseSpec, chatroomId);
  }

  private void assertProblemDetailsForUnknownChatroomException(
      WebTestClient.ResponseSpec responseSpec,
      UUID chatroomId)
  {
    responseSpec
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.type").isEqualTo("/problem/unknown-chatroom")
        .jsonPath("$.chatroomId").isEqualTo(chatroomId.toString());
  }

  @Test
  @DisplayName("Assert expected problem-details for message mutation on PUT /put/{chatroomId}/{username}/{messageId}")
  void testMessageMutationException(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String user = "foo";
    Long messageId = 66l;
    Message.MessageKey key = Message.MessageKey.of(user, messageId);
    Long serialNumberExistingMessage = 0l;
    String timeExistingMessageAsString = "2023-01-09T20:44:57.389665447";
    LocalDateTime timeExistingMessage = LocalDateTime.parse(timeExistingMessageAsString);
    String textExistingMessage = "Existing";
    String textMutatedMessage = "Mutated!";
    ChatRoomData chatRoomData = new ChatRoomData(
        Clock.systemDefaultZone(),
        chatMessageService,
        8);
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenReturn(Mono.just(chatRoomData));
    Message existingMessage = new Message(
        key,
        serialNumberExistingMessage,
        timeExistingMessage,
        textExistingMessage);
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.just(existingMessage));
    // Needed for readable error-reports, in case of a bug that leads to according unwanted call
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class)))
        .thenReturn(Mono.just(mock(Message.class)));

    // When
    client
        .put()
        .uri(
            "/{chatroomId}/{username}/{messageId}",
            chatroomId,
            user,
            messageId)
        .bodyValue(textMutatedMessage)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        // Then
        .expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.type").isEqualTo("/problem/message-mutation")
        .jsonPath("$.existingMessage.id").isEqualTo(messageId)
        .jsonPath("$.existingMessage.serial").isEqualTo(serialNumberExistingMessage)
        .jsonPath("$.existingMessage.time").isEqualTo(timeExistingMessageAsString)
        .jsonPath("$.existingMessage.user").isEqualTo(user)
        .jsonPath("$.existingMessage.text").isEqualTo(textExistingMessage)
        .jsonPath("$.mutatedText").isEqualTo(textMutatedMessage);
    verify(chatMessageService, never()).persistMessage(eq(key), any(LocalDateTime.class), any(String.class));
  }

  @Test
  @DisplayName("Assert expected problem-details for invalid username on PUT /put/{chatroomId}/{username}/{messageId}")
  void testInvalidUsernameException(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String user = "Foo";
    Long messageId = 66l;
    Message.MessageKey key = Message.MessageKey.of(user, messageId);
    String textMessage = "Hallo Welt";
    ChatRoomData chatRoomData = new ChatRoomData(
        Clock.systemDefaultZone(),
        chatMessageService,
        8);
    when(chatHomeService.getChatRoomData(any(UUID.class)))
        .thenReturn(Mono.just(chatRoomData));
    when(chatMessageService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.empty());
    // Needed for readable error-reports, in case of a bug that leads to according unwanted call
    when(chatMessageService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class)))
        .thenReturn(Mono.just(mock(Message.class)));

    // When
    client
        .put()
        .uri(
            "/{chatroomId}/{username}/{messageId}",
            chatroomId,
            user,
            messageId)
        .bodyValue(textMessage)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        // Then
        .expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.type").isEqualTo("/problem/invalid-username")
        .jsonPath("$.username").isEqualTo(user);
    verify(chatMessageService, never()).persistMessage(eq(key), any(LocalDateTime.class), any(String.class));
  }

  @Test
  @DisplayName("Assert expected problem-details for not owned shard on GET /{chatroomId}")
  void testShardNotOwnedExceptionForGetChatroom(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String instanceId = "peter";
    int shard = 666;
    when(chatHomeService.getChatRoomInfo(eq(chatroomId))).thenThrow(new ShardNotOwnedException(instanceId, shard));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/{chatroomId}", chatroomId)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Then
    assertProblemDetailsForShardNotOwnedException(responseSpec, shard);
  }

  @Test
  @DisplayName("Assert expected problem-details for not owned shard on GET /list/{chatroomId}")
  void testShardNotOwnedExceptionForListChatroom(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String instanceId = "peter";
    int shard = 666;
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenThrow(new ShardNotOwnedException(instanceId, shard));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/{chatroomId}/list", chatroomId)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Then
    assertProblemDetailsForShardNotOwnedException(responseSpec, shard);
  }

  @Test
  @DisplayName("Assert expected problem-details for not owned shard on PUT /put/{chatroomId}/{username}/{messageId}")
  void testShardNotOwnedExceptionForPutMessage(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String username = "foo";
    Long messageId = 66l;
    String instanceId = "peter";
    int shard = 666;
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenThrow(new ShardNotOwnedException(instanceId, shard));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .put()
        .uri(
            "/{chatroomId}/{username}/{messageId}",
            chatroomId,
            username,
            messageId)
        .bodyValue("bar")
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Then
    assertProblemDetailsForShardNotOwnedException(responseSpec, shard);
  }

  @Test
  @DisplayName("Assert expected problem-details for not owned shard on GET /get/{chatroomId}/{username}/{messageId}")
  void testShardNotOwnedExceptionForGetMessage(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String username = "foo";
    Long messageId = 66l;
    String instanceId = "peter";
    int shard = 666;
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenThrow(new ShardNotOwnedException(instanceId, shard));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri(
            "/{chatroomId}/{username}/{messageId}",
            chatroomId,
            username,
            messageId)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Then
    assertProblemDetailsForShardNotOwnedException(responseSpec, shard);
  }

  @Test
  @DisplayName("Assert expected problem-details for not owned shard on GET /listen/{chatroomId}")
  void testShardNotOwnedExceptionForListenChatroom(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    String instanceId = "peter";
    int shard = 666;
    when(chatHomeService.getChatRoomData(eq(chatroomId))).thenThrow(new ShardNotOwnedException(instanceId, shard));

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/{chatroomId}/listen", chatroomId)
        // .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON) << TODO: Does not work!
        .exchange();

    // Then
    assertProblemDetailsForShardNotOwnedException(responseSpec, shard);
  }

  private void assertProblemDetailsForShardNotOwnedException(
      WebTestClient.ResponseSpec responseSpec,
      int shard)
  {
    responseSpec
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.type").isEqualTo("/problem/shard-not-owned")
        .jsonPath("$.shard").isEqualTo(shard);
  }
}
