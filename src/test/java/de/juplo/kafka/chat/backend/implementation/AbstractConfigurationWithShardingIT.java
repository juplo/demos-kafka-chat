package de.juplo.kafka.chat.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;

import static org.hamcrest.Matchers.endsWith;


public abstract class AbstractConfigurationWithShardingIT extends AbstractConfigurationIT
{
  @Test
  @DisplayName("A PUT-message for a not owned shard yields 404 - NOT FOUND")
  void testNotFoundForPutMessageToAChatRoomInNotOwnedShard()
  {
    String otherChatRoomId = "4e7246a6-29ae-43ea-b56f-669c3481ac19";
    int shard = 0;

    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
          webTestClient
              .put()
              .uri("/{chatRoomId}/otto/66", otherChatRoomId)
              .contentType(MediaType.TEXT_PLAIN)
              .accept(MediaType.APPLICATION_JSON)
              .bodyValue("The devil rules route 66")
              .exchange()
              .expectStatus().is5xxServerError()
              .expectBody()
              .jsonPath("$.type").value(endsWith("/problem/shard-not-owned"))
              .jsonPath("$.shard").isEqualTo(shard));
  }
}
