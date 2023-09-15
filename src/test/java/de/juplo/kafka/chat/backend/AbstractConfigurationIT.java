package de.juplo.kafka.chat.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;

import static org.hamcrest.Matchers.endsWith;


public abstract class AbstractConfigurationIT
{
  final static String EXISTING_CHATROOM = "5c73531c-6fc4-426c-adcb-afc5c140a0f7";


  @LocalServerPort
  int port;
  @Autowired
  WebTestClient webTestClient;


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
    String otherChatRoomId = "7f59ec77-832e-4a17-8d22-55ef46242c17";

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
                  otherChatRoomId)
              .contentType(MediaType.TEXT_PLAIN)
              .accept(MediaType.APPLICATION_JSON)
              .bodyValue("The devil rules route 66")
              .exchange()
              .expectStatus().isNotFound()
              .expectBody()
                .jsonPath("$.type").value(endsWith("/problem/unknown-chatroom"))
                .jsonPath("$.chatroomId").isEqualTo(otherChatRoomId);
        });
  }
}
