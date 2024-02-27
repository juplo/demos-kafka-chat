package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;


@Slf4j
class KafkaHandoverIT extends AbstractHandoverIT
{
  @BeforeEach
  void setUpWebClient() throws IOException, InterruptedException
  {
    kafka.start();
    haproxy.start();

    Container.ExecResult result;
    result = kafka.execInContainer(
        "kafka-topics",
        "--bootstrap-server",
        "kafka:9999",
        "--create",
        "--topic",
        "info_channel",
        "--partitions",
        "3");
    log.info(
        "EXIT-CODE={}, STDOUT={}, STDERR={}",
        result.getExitCode(),
        result.getStdout(),
        result.getStdout());
    result = kafka.execInContainer(
        "kafka-topics",
        "--bootstrap-server",
        "kafka:9999",
        "--create",
        "--topic",
        "data_channel",
        "--partitions",
        "10");
    log.info(
        "EXIT-CODE={}, STDOUT={}, STDERR={}",
        result.getExitCode(),
        result.getStdout(),
        result.getStdout());

    backend1.start();
    // backend2.start();
    // backend3.start();

    Integer port = haproxy.getMappedPort(8400);
    webClient = WebClient.create("http://localhost:" + port);

    Awaitility
        .await()
        .atMost(Duration.ofMinutes(10))
        .until(() -> WebClient
            .create("http://localhost:" + backend1.getMappedPort(8080))
            .get()
            .uri("/actuator/health")
            .exchangeToMono(response ->
            {
              if (response.statusCode().equals(HttpStatus.OK))
              {
                return response
                    .bodyToMono(StatusTo.class)
                    .map(StatusTo::getStatus)
                    .map(status -> status.equalsIgnoreCase("UP"));
              }
              else
              {
                return Mono.just(false);
              }
            })
            .block());

    haproxy
        .getDockerClient()
        .killContainerCmd(haproxy.getContainerId())
        .withSignal("HUP")
        .exec();


    Awaitility
        .await()
        .atMost(Duration.ofMinutes(10))
        .until(() -> webClient
            .get()
            .uri("/actuator/health")
            .exchangeToMono(response ->
            {
              if (response.statusCode().equals(HttpStatus.OK))
              {
                return response
                    .bodyToMono(StatusTo.class)
                    .map(StatusTo::getStatus)
                    .map(status -> status.equalsIgnoreCase("UP"));
              }
              else
              {
                return Mono.just(false);
              }
            })
            .block());
  }


  Network network = Network.newNetwork();

  KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
          .withNetwork(network)
          .withNetworkAliases("kafka")
          .withListener(() -> "kafka:9999")
          .withKraft()
          .waitingFor(Wait.forLogMessage(".*Kafka\\ Server\\ started.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("KAFKA"));

  GenericContainer backend1 =
      new GenericContainer(DockerImageName.parse("juplo/chat-backend:0.0.1-SNAPSHOT"))
          .withImagePullPolicy(NEVER_PULL)
          .withNetwork(network)
          .withNetworkAliases("backend-1")
          .withCommand(
              "--chat.backend.instance-id=backend_1",
              "--chat.backend.services=kafka",
              "--chat.backend.kafka.bootstrap-servers=kafka:9999",
              "--chat.backend.kafka.instance-uri=http://backend-1:8080",
              "--chat.backend.kafka.num-partitions=10",
              "--chat.backend.kafka.client-id-prefix=B1",
              "--chat.backend.kafka.haproxy-runtime-api=haproxy:8401",
              "--chat.backend.kafka.haproxy-map=/usr/local/etc/haproxy/sharding.map"
          )
          .withExposedPorts(8080)
          .waitingFor(Wait.forLogMessage(".*Started\\ ChatBackendApplication.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("BACKEND-1"));

  GenericContainer backend2 =
      new GenericContainer(DockerImageName.parse("juplo/chat-backend:0.0.1-SNAPSHOT"))
          .withImagePullPolicy(NEVER_PULL)
          .withNetwork(network)
          .withNetworkAliases("backend-2")
          .withCommand(
              "--chat.backend.instance-id=backend_2",
              "--chat.backend.services=kafka",
              "--chat.backend.kafka.bootstrap-servers=kafka:9999",
              "--chat.backend.kafka.instance-uri=http://backend-2:8080",
              "--chat.backend.kafka.num-partitions=10",
              "--chat.backend.kafka.client-id-prefix=B2",
              "--chat.backend.kafka.haproxy-runtime-api=haproxy:8401",
              "--chat.backend.kafka.haproxy-map=/usr/local/etc/haproxy/sharding.map"
          )
          .withExposedPorts(8080)
          .waitingFor(Wait.forLogMessage(".*Started\\ ChatBackendApplication.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("BACKEND-2"));

  GenericContainer backend3 =
      new GenericContainer(DockerImageName.parse("juplo/chat-backend:0.0.1-SNAPSHOT"))
          .withImagePullPolicy(NEVER_PULL)
          .withNetwork(network)
          .withNetworkAliases("backend-3")
          .withCommand(
              "--chat.backend.instance-id=backend_3",
              "--chat.backend.services=kafka",
              "--chat.backend.kafka.bootstrap-servers=kafka:9999",
              "--chat.backend.kafka.instance-uri=http://backend-3:8080",
              "--chat.backend.kafka.num-partitions=10",
              "--chat.backend.kafka.client-id-prefix=B3",
              "--chat.backend.kafka.haproxy-runtime-api=haproxy:8401",
              "--chat.backend.kafka.haproxy-map=/usr/local/etc/haproxy/sharding.map"
          )
          .withExposedPorts(8080)
          .waitingFor(Wait.forLogMessage(".*Started\\ ChatBackendApplication.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("BACKEND-3"));

  GenericContainer haproxy =
      new GenericContainer(DockerImageName.parse("haproxytech/haproxy-debian:2.8"))
          .withNetwork(network)
          .withNetworkAliases("haproxy")
          .withClasspathResourceMapping(
              "haproxy.cfg",
              "/usr/local/etc/haproxy/haproxy.cfg",
              BindMode.READ_ONLY)
          .withClasspathResourceMapping(
              "sharding.map",
              "/usr/local/etc/haproxy/sharding.map",
              BindMode.READ_WRITE)
          .withExposedPorts(8400, 8401, 8404)
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("HAPROXY"));
}
