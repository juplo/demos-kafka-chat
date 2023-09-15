package de.juplo.kafka.chat.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;


@Slf4j
public abstract class AbstractConfigurationIT
{
  final static String EXISTING_CHATROOM = "5c73531c-6fc4-426c-adcb-afc5c140a0f7";
  String NONEXISTENT_CHATROOM = "7f59ec77-832e-4a17-8d22-55ef46242c17";


  @LocalServerPort
  int port;
  @Autowired
  WebTestClient webTestClient;
  @Autowired
  ObjectMapper objectMapper;


  @BeforeEach
  void waitForApp()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri(
                  "http://localhost:{port}/actuator/health",
                  port)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.status").isEqualTo("UP");
        });
  }

  @Test
  @DisplayName("Restored chat-rooms can be listed")
  void testRestoredChatRoomsCanBeListed()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri(
                  "http://localhost:{port}/list",
                  port)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("FOO");
        });
  }

  @Test
  @DisplayName("Details as expected for restored chat-room")
  void testRestoredChatRoomHasExpectedDetails()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri(
                  "http://localhost:{port}/{chatRoomId}",
                  port,
                  EXISTING_CHATROOM)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.name").isEqualTo("FOO");
        });
  }

  @Test
  @DisplayName("Restored message from Ute has expected Text")
  void testRestoredMessageForUteHasExpectedText()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri(
                  "http://localhost:{port}/{chatRoomId}/ute/1",
                  port,
                  EXISTING_CHATROOM)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.text").isEqualTo("Ich bin Ute...");
        });
  }

  @Test
  @DisplayName("Restored message from Peter has expected Text")
  void testRestoredMessageForPeterHasExpectedText()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri(
                  "http://localhost:{port}/{chatRoomId}/peter/1",
                  port,
                  EXISTING_CHATROOM)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.text").isEqualTo("Hallo, ich heiße Peter!");
        });
  }

  @Test
  @DisplayName("A PUT-message for a non-existent chat-room yields 404 NOT FOUND")
  void testNotFoundForPutMessageToNonExistentChatRoom()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .put()
              .uri(
                  "http://localhost:{port}/{chatRoomId}/otto/66",
                  port,
                  NONEXISTENT_CHATROOM)
              .contentType(MediaType.TEXT_PLAIN)
              .accept(MediaType.APPLICATION_JSON)
              .bodyValue("The devil rules route 66")
              .exchange()
              .expectStatus().isNotFound()
              .expectBody()
                .jsonPath("$.type").value(endsWith("/problem/unknown-chatroom"))
                .jsonPath("$.chatroomId").isEqualTo(NONEXISTENT_CHATROOM);
        });
  }

  @Test
  @DisplayName("A message can be put into a newly created chat-room")
  void testPutMessageInNewChatRoom() throws IOException
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          byte[] result = webTestClient
              .post()
              .uri("http://localhost:{port}/create", port)
              .contentType(MediaType.TEXT_PLAIN)
              .bodyValue("bar")
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.name").isEqualTo("bar")
                // The hard must not be asserted, because not all implementations ar aware of it
                // .jsonPath("$.shard").isEqualTo(Integer.valueOf(2))
              .returnResult()
              .getResponseBody();
          ChatRoomInfoTo chatRoomInfo = objectMapper.readValue(result, ChatRoomInfoTo.class);
          UUID chatRoomId = chatRoomInfo.getId();
          webTestClient
              .put()
              .uri(
                  "http://localhost:{port}/{chatRoomId}/nerd/7",
                  port,
                  chatRoomId)
              .contentType(MediaType.TEXT_PLAIN)
              .accept(MediaType.APPLICATION_JSON)
              .bodyValue("Hello world!")
              .exchange()
              .expectStatus().isOk()
              .expectBody()
                .jsonPath("$.id").isEqualTo(Integer.valueOf(7))
                .jsonPath("$.user").isEqualTo("nerd")
                .jsonPath("$.text").isEqualTo("Hello world!");
        });
  }
}
