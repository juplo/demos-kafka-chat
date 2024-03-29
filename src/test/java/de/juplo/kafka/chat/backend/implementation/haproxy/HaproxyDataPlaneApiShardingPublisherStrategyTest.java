package de.juplo.kafka.chat.backend.implementation.haproxy;

import com.adelean.inject.resources.junit.jupiter.GivenTextResource;
import com.adelean.inject.resources.junit.jupiter.TestWithResources;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@TestWithResources
public class HaproxyDataPlaneApiShardingPublisherStrategyTest
{
  final static String MAP_NAME = "sharding";
  final static String INSTANCE_ID = "backend_3";
  final static int SHARD = 4;


  MockWebServer mockHaproxy;
  WebClient webClient;

  @GivenTextResource("de/juplo/kafka/chat/backend/implementation/haproxy/maps_entries__4__put.json")
  String maps_entries__4__put;
  @GivenTextResource("de/juplo/kafka/chat/backend/implementation/haproxy/maps_entries__4__put__error.json")
  String maps_entries__4__put__error;


  @BeforeEach
  void setUp() throws IOException
  {
    mockHaproxy = new MockWebServer();
    mockHaproxy.start();
    webClient = WebClient
        .builder()
        .baseUrl(String.format("http://localhost:%s/v2", mockHaproxy.getPort()))
        .build();
  }

  @AfterEach
  void tearDown() throws IOException
  {
    mockHaproxy.shutdown();
  }


  @DisplayName("The expected result is yielded on successful publishing")
  @Test
  void testExpectedResultOnSuccessfulPublishing()
  {
    // Given
    mockHaproxy.enqueue(new MockResponse()
        .setStatus("HTTP/1.1 200 OK")
        .setBody(maps_entries__4__put)
        .addHeader("Content-Type", "application/json"));

    // When
    HaproxyDataPlaneApiShardingPublisherStrategy shardingPublisherStrategy =
        new HaproxyDataPlaneApiShardingPublisherStrategy(webClient, MAP_NAME, INSTANCE_ID);
    Mono<String> result = shardingPublisherStrategy.publishOwnership(SHARD);

    // Then
    assertThat(result.block(Duration.ofSeconds(1)))
        .isEqualTo(INSTANCE_ID);
  }

  @DisplayName("The expected error is raised on failed publishing")
  @Test
  void testExpectedResultOnFailedPublishing()
  {
    // Given
    mockHaproxy.enqueue(new MockResponse()
        .setStatus("HTTP/1.1 400 Bad Request")
        .setBody(maps_entries__4__put__error)
        .addHeader("Content-Type", "application/json"));

    // When
    HaproxyDataPlaneApiShardingPublisherStrategy shardingPublisherStrategy =
        new HaproxyDataPlaneApiShardingPublisherStrategy(webClient, MAP_NAME, INSTANCE_ID);
    Mono<String> result = shardingPublisherStrategy
        .publishOwnership(SHARD)
        .onErrorResume(throwable -> Mono.just(throwable.getMessage()));

    // Then
    assertThat(result.block(Duration.ofSeconds(1)))
        .isEqualTo("Evil Error -- BOOM!");
  }
}
