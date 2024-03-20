package de.juplo.kafka.chat.backend.implementation.haproxy;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

import static de.juplo.kafka.chat.backend.implementation.haproxy.HaproxyDataPlaneApiShardingPublisherStrategy.MAP_ENTRY_PATH;
import static de.juplo.kafka.chat.backend.implementation.haproxy.HaproxyDataPlaneApiShardingPublisherStrategy.MAP_PARAM;
import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
@Slf4j
public class HaproxyDataPlaneApiShardingPublisherStrategyIT
{
  @Test
  void test() throws InterruptedException
  {
    Mono<String> result = shardingPublisherStrategy.publishOwnership(SHARD);

    assertThat(result.block(Duration.ofSeconds(5)))
        .isEqualTo(INSTANCE_ID);
    assertThat(getMapEntryValueForKey(SHARD).block(Duration.ofSeconds(5)))
        .isEqualTo(INSTANCE_ID);

    HAPROXY
        .getDockerClient()
        .killContainerCmd(HAPROXY.getContainerId())
        .withSignal("HUP")
        .exec();

    Thread.sleep(1000); // << No clue, how to detect that the reload is complete

    assertThat(getMapEntryValueForKey(SHARD).block(Duration.ofSeconds(5)))
        .isEqualTo(INSTANCE_ID);
  }


  private Mono<String> getMapEntryValueForKey(int key)
  {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path(MAP_ENTRY_PATH)
            .queryParam(MAP_PARAM, MAP_NAME)
            .build(key))
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToMono(response ->
        {
          if (response.statusCode().equals(HttpStatus.OK))
          {
            return response.bodyToMono(MapEntryTo.class);
          }
          else
          {
            return response.createError();
          }
        })
        .retryWhen(Retry.fixedDelay(15, Duration.ofSeconds(1)))
        .map(entry -> entry.value());
  }


  WebClient webClient;
  HaproxyDataPlaneApiShardingPublisherStrategy shardingPublisherStrategy;


  @BeforeEach
  void setUpWebClient()
  {
    webClient = WebClient
        .builder()
        .baseUrl("http://localhost:" + HAPROXY.getMappedPort(5555) + "/v2/")
        .defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth("juplo", "juplo"))
        .build();

    shardingPublisherStrategy = new HaproxyDataPlaneApiShardingPublisherStrategy(
        webClient,
        MAP_NAME,
        INSTANCE_ID);
  }


  static final String MAP_NAME = "sharding";
  static final String INSTANCE_ID = "foo";
  static final int SHARD = 6;

  @Container
  static final GenericContainer HAPROXY =
      new GenericContainer(DockerImageName.parse("haproxytech/haproxy-debian:2.8"))
          .withCommand("-f", "/etc/haproxy")
          .withNetwork(Network.newNetwork())
          .withNetworkAliases("haproxy")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("haproxy.cfg"),
              "/etc/haproxy/haproxy.cfg")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("dataplaneapi.yml"),
              "/etc/haproxy/dataplaneapi.yml")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("sharding.map"),
              "/etc/haproxy/maps/sharding.map")
          .withExposedPorts(8400, 8401, 8404, 5555)
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("HAPROXY"));
}
