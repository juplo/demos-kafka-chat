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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureWebTestClient
@Slf4j
public class ChatBackendControllerTest
{
  @MockBean
  ChatHome chatHome;

  @Test
  @DisplayName("Assert expected problem-details for unknown chatroom on GET /list/{chatroomId}")
  void testUnknownChatroomExceptionForListChatroom(@Autowired WebTestClient client)
  {
    // Given
    UUID chatroomId = UUID.randomUUID();
    when(chatHome.getChatRoom(any(UUID.class)))
        .thenReturn(Mono.error(() -> new UnknownChatroomException(chatroomId)));

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
    when(chatHome.getChatRoom(any(UUID.class)))
        .thenReturn(Mono.error(() -> new UnknownChatroomException(chatroomId)));

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
    when(chatHome.getChatRoom(any(UUID.class)))
        .thenReturn(Mono.error(() -> new UnknownChatroomException(chatroomId)));

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
    when(chatHome.getChatRoom(any(UUID.class)))
        .thenReturn(Mono.error(() -> new UnknownChatroomException(chatroomId)));

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
    when(chatHome.getChatRoom(any(UUID.class)))
        .thenReturn(Mono.error(() -> new UnknownChatroomException(chatroomId)));

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
    String username = "foo";
    Long messageId = 66l;
    ChatRoom chatRoom = mock(ChatRoom.class);
    when(chatHome.getChatRoom(any(UUID.class)))
        .thenReturn(Mono.just(chatRoom));
    Message.MessageKey key = Message.MessageKey.of("foo", 1l);
    LocalDateTime timestamp = LocalDateTime.now();
    Message existing = new Message(key, 0l, timestamp, "Existing");
    when(chatRoom.addMessage(any(Long.class), any(String.class), any(String.class)))
        .thenReturn(Mono.error(() -> new MessageMutationException(existing, "Mutated!")));

    // When
    client
        .put()
        .uri(
            "/put/{chatroomId}/{username}/{messageId}",
            chatroomId,
            username,
            messageId)
        .bodyValue("bar")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        // Then
        .expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.type").isEqualTo("/problem/message-mutation")
        .jsonPath("$.existingMessage.text").isEqualTo("Existing")
        .jsonPath("$.mutatedText").isEqualTo("Mutated!");
  }
}
