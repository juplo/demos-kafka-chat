package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.implementation.haproxy.MapEntryTo;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static de.juplo.kafka.chat.backend.implementation.haproxy.HaproxyDataPlaneApiShardingPublisherStrategy.MAP_PARAM;


@Slf4j
public abstract class AbstractHandoverITContainers
{
  static final ImagePullPolicy NEVER_PULL = imageName -> false;


  final Network network = Network.newNetwork();
  final GenericContainer haproxy, backend1, backend2, backend3;


  AbstractHandoverITContainers()
  {
    haproxy = createHaproxyContainer();
    haproxy.start();

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

    String backendUri = "http://localhost:" + backend.getMappedPort(8080);

    Instant before, after;

    before = Instant.now();
    HttpStatusCode statusCode = WebClient
        .create(backendUri)
        .get()
        .uri("/actuator/health")
        .exchangeToMono(response ->
        {
          log.info("{} responded with {}", backendUri, response.statusCode());
          return Mono.just(response.statusCode());
        })
        .flatMap(status -> switch (status.value())
        {
          case 200, 503 -> Mono.just(status);
          default -> Mono.error(new RuntimeException(status.toString()));
        })
        .retryWhen(Retry.backoff(30, Duration.ofSeconds(1)))
        .block();
    after = Instant.now();
    log.info("Took {} to reach status {}", Duration.between(before, after), statusCode);

    before = Instant.now();
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(45))
        .until(() -> WebClient
            .create(backendUri)
            .get()
            .uri("/actuator/health")
            .exchangeToMono(response ->
            {
              log.info("{} responded with {}", backendUri, response.statusCode());
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
    after = Instant.now();
    log.info("Took {} until the backend reported status UP", Duration.between(before, after));

    haproxy
        .getDockerClient()
        .killContainerCmd(haproxy.getContainerId())
        .withSignal("HUP")
        .exec();

    before = Instant.now();
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .until(() -> WebClient
            .create("http://localhost:" + haproxy.getMappedPort(8400))
            .get()
            .uri("/actuator/health")
            .exchangeToMono(response ->
            {
              log.info("{} responded with {}", backendUri, response.statusCode());
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
    after = Instant.now();
    log.info("Took {} until haproxy reported status UP", Duration.between(before, after));

    before = Instant.now();
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
                  "No progress for {}: sent-before={}, sent-total={}, map: {}",
                  testWriter,
                  numSentMessages[i],
                  sentTotal,
                  readHaproxyMap());
              return false;
            }
          }

          return true;
        });
    after = Instant.now();
    log.info("Took {} until all writers made some progress", Duration.between(before, after));
  }

  abstract String[] getBackendCommand();

  final GenericContainer createHaproxyContainer()
  {
    return new GenericContainer(DockerImageName.parse("haproxytech/haproxy-debian:2.8"))
        .withCommand("-f", "/etc/haproxy")
        .withNetwork(network)
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

  private String readHaproxyMap()
  {
    return createHaproxyWebClient()
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/services/haproxy/runtime/maps_entries")
            .queryParam(MAP_PARAM, MAP_NAME)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToFlux(response ->
        {
          if (response.statusCode().equals(HttpStatus.OK))
          {
            return response.bodyToFlux(MapEntryTo.class);
          }
          else
          {
            return response.<MapEntryTo>createError().flux();
          }
        })
        .map(entry -> entry.key() + "=" + entry.value())
        .reduce((a, b) -> a + ", " + b)
        .block();
  }

  private WebClient createHaproxyWebClient()
  {
    return WebClient
        .builder()
        .baseUrl("http://localhost:" + haproxy.getMappedPort(5555) + "/v2/")
        .defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth("juplo", "juplo"))
        .build();
  }

  static final String MAP_NAME = "sharding";
}
