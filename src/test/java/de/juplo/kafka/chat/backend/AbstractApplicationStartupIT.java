package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;

import static pl.rzrz.assertj.reactor.Assertions.assertThat;


@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Slf4j
public abstract class AbstractApplicationStartupIT
{
  @Autowired
  WebTestClient webTestClient;


  @Test
  @DisplayName("Applications starts when no data is available (fresh install)")
  void testStartup() throws IOException
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> webTestClient
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.status").isEqualTo("UP"));
  }

  @Test
  @DisplayName("Chat-rooms can be listed and returns an empty list")
  void testListChatRooms() throws IOException
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          Flux<ChatRoomInfoTo> result = webTestClient
              .get()
              .uri("/list")
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .returnResult(ChatRoomInfoTo.class)
              .getResponseBody()
              .doOnNext(chatRoomInfoTo ->
              {
                log.debug("Found chat-room {}", chatRoomInfoTo);
              });

          assertThat(result).emitsExactly();
        });
  }
}
