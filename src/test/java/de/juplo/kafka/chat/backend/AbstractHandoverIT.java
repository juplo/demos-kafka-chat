package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Testcontainers
@Slf4j
public abstract class AbstractHandoverIT
{
  static final int NUM_CHATROOMS = 23;
  static final int NUM_CLIENTS = 17;


  private final AbstractHandoverITContainers containers;
  private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_CLIENTS + 1);


  AbstractHandoverIT(AbstractHandoverITContainers containers)
  {
    this.containers = containers;
  }


  @Test
  void test() throws InterruptedException
  {
    ChatRoomInfoTo[] chatRooms = Flux
        .range(0, NUM_CHATROOMS)
        .flatMap(i -> createChatRoom("room-" + i))
        .toStream()
        .toArray(size -> new ChatRoomInfoTo[size]);

    int port = containers.haproxy.getMappedPort(8400);

    TestWriter[] testWriters = Flux
        .range(0, NUM_CLIENTS)
        .map(i -> new TestWriter(
            port,
            chatRooms[i % NUM_CHATROOMS],
            "user-" + Integer.toString(i)))
        .doOnNext(testClient -> executorService.execute(testClient))
        .toStream()
        .toArray(size -> new TestWriter[size]);

    TestListener testListener = new TestListener(port, chatRooms);
    executorService.execute(testListener);

    Thread.sleep(2000);

    Arrays
        .stream(testWriters)
        .forEach(testClient -> testClient.running = false);
    testListener.running = false;
  }

  Mono<ChatRoomInfoTo> createChatRoom(String name)
  {
    return webClient
        .post()
        .uri("/create")
        .contentType(MediaType.TEXT_PLAIN)
        .bodyValue(name)
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToMono(response ->
        {
          if (response.statusCode().equals(HttpStatus.OK))
          {
            return response.bodyToMono(ChatRoomInfoTo.class);
          }
          else
          {
            return response.createError();
          }
        });
  }


  WebClient webClient;

  @BeforeEach
  void setUp() throws Exception
  {
    containers.setUp();

    Integer port = containers.haproxy.getMappedPort(8400);
    webClient = WebClient.create("http://localhost:" + port);
  }
}
