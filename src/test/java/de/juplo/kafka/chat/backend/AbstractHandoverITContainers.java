package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;


@Slf4j
public abstract class AbstractHandoverITContainers
{
  static final ImagePullPolicy NEVER_PULL = imageName -> false;


  final Network network = Network.newNetwork();
  final GenericContainer haproxy, backend1, backend2, backend3;


  AbstractHandoverITContainers()
  {
    haproxy = createHaproxyContainer();
    backend1 = createBackendContainer("1");
    backend2 = createBackendContainer("2");
    backend3 = createBackendContainer("3");
  }


  void setUpExtra() throws Exception
  {
    log.info("This setup does not need any extra containers");
  }

  void setUp() throws Exception
  {
    setUpExtra();
    haproxy.start();
  }

  void startBackend(
      GenericContainer backend,
      TestWriter[] testWriters)
  {
    backend.start();

    int[] numSentMessages = Arrays
        .stream(testWriters)
        .mapToInt(testWriter -> testWriter.getNumSentMessages())
        .toArray();

    Awaitility
        .await()
        .atMost(Duration.ofSeconds(30))
        .until(() -> WebClient
            .create("http://localhost:" + backend.getMappedPort(8080))
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
        .atMost(Duration.ofSeconds(30))
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

    Awaitility
        .await()
        .atMost(Duration.ofSeconds(30))
        .until(() ->
        {
          for (int i = 0; i < testWriters.length; i++)
          {
            TestWriter testWriter = testWriters[i];
            int sentTotal = testWriter.getNumSentMessages();
            if (sentTotal == numSentMessages[i])
            {
              log.info(
                  "No progress for {}: sent-before={}, sent-total={}",
                  testWriter,
                  numSentMessages[i],
                  sentTotal);
              return false;
            }
          }

          return true;
        });
  }

  abstract String[] getBackendCommand();

  final GenericContainer createHaproxyContainer()
  {
    return new GenericContainer(DockerImageName.parse("haproxytech/haproxy-debian:2.8"))
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

  final GenericContainer createBackendContainer(String id)
  {
    return new GenericContainer(DockerImageName.parse("juplo/chat-backend:0.0.1-SNAPSHOT"))
      .withImagePullPolicy(NEVER_PULL)
      .withNetwork(network)
      .withNetworkAliases("backend-ID".replaceAll("ID", id))
      .withCommand(Arrays.stream(getBackendCommand())
          .map(commandPart -> commandPart.replaceAll("ID", id))
          .toArray(size -> new String[size]))
      .withExposedPorts(8080)
      .waitingFor(Wait.forLogMessage(".*Started\\ ChatBackendApplication.*\\n", 1))
      .withLogConsumer(new Slf4jLogConsumer(
          log,
          true
          )
          .withPrefix("BACKEND-ID".replaceAll("ID", id)));
  }
}
