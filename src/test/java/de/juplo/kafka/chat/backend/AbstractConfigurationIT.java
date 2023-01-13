package de.juplo.kafka.chat.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;


public abstract class AbstractConfigurationIT
{
  @LocalServerPort
  int port;
  @Autowired
  WebTestClient webTestClient;

  @Test
  @DisplayName("The app starts, the data is restored and accessible")
  void test()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri("http://localhost:{port}/actuator/health", port)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.status").isEqualTo("UP");
          webTestClient
              .get()
              .uri("http://localhost:{port}/5c73531c-6fc4-426c-adcb-afc5c140a0f7", port)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.name").isEqualTo("FOO");
          webTestClient
              .get()
              .uri("http://localhost:{port}/5c73531c-6fc4-426c-adcb-afc5c140a0f7/ute/1", port)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.text").isEqualTo("Ich bin Ute...");
          webTestClient
              .get()
              .uri("http://localhost:{port}/5c73531c-6fc4-426c-adcb-afc5c140a0f7/peter/1", port)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.text").isEqualTo("Hallo, ich heiße Peter!");
        });
  }
}
