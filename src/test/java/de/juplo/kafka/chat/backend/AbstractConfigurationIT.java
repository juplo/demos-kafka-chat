package de.juplo.kafka.chat.backend;

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
  @LocalServerPort
  int port;
  @Autowired
  WebTestClient webTestClient;


  @Test
  @DisplayName("The app starts, the data is restored and accessible")
  void testAppStartsDataIsRestoredAndAccessible()
  {
    String chatRoomId = "5c73531c-6fc4-426c-adcb-afc5c140a0f7";

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
          webTestClient
              .get()
              .uri(
                  "http://localhost:{port}/{chatRoomId}",
                  port,
                  chatRoomId)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.name").isEqualTo("FOO");
          webTestClient
              .get()
              .uri(
                  "http://localhost:{port}/{chatRoomId}/ute/1",
                  port,
                  chatRoomId)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.text").isEqualTo("Ich bin Ute...");
          webTestClient
              .get()
              .uri(
                  "http://localhost:{port}/{chatRoomId}/peter/1",
                  port,
                  chatRoomId)
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
