package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.stream.IntStream;


@Testcontainers
@Slf4j
public abstract class AbstractHandoverIT
{
  static final ImagePullPolicy NEVER_PULL = imageName -> false;


  @Test
  void test() throws InterruptedException
  {
    ChatRoomInfoTo chatRoom = createChatRoom("bar").block();
    User user = new User("nerd");
    IntStream
        .rangeClosed(1,100)
        .mapToObj(i ->sendMessage(chatRoom, user, "Message #" + i))
        .map(result -> result
            .map(MessageTo::toString)
            .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(1)))
            .block())
        .forEach(result -> log.info("{}", result));

    receiveMessages(chatRoom)
        .take(100)
        .doOnNext(message -> log.info("message: {}", message))
        .then()
        .block();
  }


  abstract void setUpExtra() throws IOException, InterruptedException;

  @BeforeEach
  void setUp() throws Exception
  {
    setUpExtra();
    haproxy.start();
    backend1.start();
    // backend2.start();
    // backend3.start();

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
        .until(() -> WebClient
            .create("http://localhost:" + haproxy.getMappedPort(8400))
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

  abstract String[] getCommandBackend1();
  GenericContainer backend1 =
      new GenericContainer(DockerImageName.parse("juplo/chat-backend:0.0.1-SNAPSHOT"))
          .withImagePullPolicy(NEVER_PULL)
          .withNetwork(network)
          .withNetworkAliases("backend-1")
          .withCommand(getCommandBackend1())
          .withExposedPorts(8080)
          .waitingFor(Wait.forLogMessage(".*Started\\ ChatBackendApplication.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("BACKEND-1"));

  abstract String[] getCommandBackend2();
  GenericContainer backend2 =
      new GenericContainer(DockerImageName.parse("juplo/chat-backend:0.0.1-SNAPSHOT"))
          .withImagePullPolicy(NEVER_PULL)
          .withNetwork(network)
          .withNetworkAliases("backend-2")
          .withCommand(getCommandBackend2())
          .withExposedPorts(8080)
          .waitingFor(Wait.forLogMessage(".*Started\\ ChatBackendApplication.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("BACKEND-2"));

  abstract String[] getCommandBackend3();
  GenericContainer backend3 =
      new GenericContainer(DockerImageName.parse("juplo/chat-backend:0.0.1-SNAPSHOT"))
          .withImagePullPolicy(NEVER_PULL)
          .withNetwork(network)
          .withNetworkAliases("backend-3")
          .withCommand(getCommandBackend3())
          .withExposedPorts(8080)
          .waitingFor(Wait.forLogMessage(".*Started\\ ChatBackendApplication.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("BACKEND-3"));


  @EqualsAndHashCode
  @ToString
  class User
  {
    @Getter
    private final String name;
    private int serial = 0;


    User (String name)
    {
      this.name = name;
    }


    int nextSerial()
    {
      return ++serial;
    }
  }

  @Getter
  @Setter
  static class StatusTo
  {
    String status;
  }
}
