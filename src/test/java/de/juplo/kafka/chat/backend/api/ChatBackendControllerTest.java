package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.*;
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


@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureWebTestClient
@Slf4j
public class ChatBackendControllerTest
{
  @MockBean
  ChatHomeService chatHomeService;
  @MockBean
  ChatRoomService chatRoomService;

  @Test
  @DisplayName("Assert expected problem-details for unknown chatroom on GET /list/{chatroomId}")
  void testUnknownChatroomExceptionForListChatroom(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    when(chatHomeService.getChatRoom(any(UUID.class))).thenReturn(Mono.empty());

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/list/{chatroomId}", chatroomId)
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
    when(chatHomeService.getChatRoom(any(UUID.class))).thenReturn(Mono.empty());

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/get/{chatroomId}", chatroomId)
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
    when(chatHomeService.getChatRoom(any(UUID.class))).thenReturn(Mono.empty());

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .put()
        .uri(
            "/put/{chatroomId}/{username}/{messageId}",
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
    when(chatHomeService.getChatRoom(any(UUID.class))).thenReturn(Mono.empty());

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri(
            "/get/{chatroomId}/{username}/{messageId}",
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
    when(chatHomeService.getChatRoom(any(UUID.class))).thenReturn(Mono.empty());

    // When
    WebTestClient.ResponseSpec responseSpec = client
        .get()
        .uri("/listen/{chatroomId}", chatroomId)
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
  void testMessageMutationException(@Autowired WebTestClient client) throws Exception
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
    ChatRoom chatRoom = new ChatRoom(
        chatroomId,
        "Test-ChatRoom",
        Clock.systemDefaultZone(),
        chatRoomService, 8);
    when(chatHomeService.getChatRoom(any(UUID.class))).thenReturn(Mono.just(chatRoom));
    Message existingMessage = new Message(
        key,
        serialNumberExistingMessage,
        timeExistingMessage,
        textExistingMessage);
    when(chatRoomService.getMessage(any(Message.MessageKey.class)))
        .thenReturn(Mono.just(existingMessage));
    // Needed for readable error-reports, in case of a bug that leads to according unwanted call
    when(chatRoomService.persistMessage(any(Message.MessageKey.class), any(LocalDateTime.class), any(String.class)))
        .thenReturn(mock(Message.class));

    // When
    client
        .put()
        .uri(
            "/put/{chatroomId}/{username}/{messageId}",
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
    verify(chatRoomService, never()).persistMessage(eq(key), any(LocalDateTime.class), any(String.class));
  }
}
