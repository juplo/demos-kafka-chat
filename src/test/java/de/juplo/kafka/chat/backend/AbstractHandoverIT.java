package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;


@Testcontainers
@Slf4j
public abstract class AbstractHandoverIT
{
  static final int NUM_CHATROOMS = 23;
  static final int NUM_CLIENTS = 17;


  private final AbstractHandoverITContainers containers;


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

    CompletableFuture<Void>[] testWriterFutures = new CompletableFuture[NUM_CLIENTS];
    TestWriter[] testWriters = new TestWriter[NUM_CLIENTS];
    for (int i = 0; i < NUM_CLIENTS; i++)
    {
      TestWriter testWriter = new TestWriter(
          port,
          chatRooms[i % NUM_CHATROOMS],
          "user-" + i);
      testWriters[i] = testWriter;
      testWriterFutures[i] = testWriter
          .run()
          .toFuture();
    }

    TestListener testListener = new TestListener(port, chatRooms);
    CompletableFuture<Void> testListenerFuture = testListener
        .run()
        .toFuture();

    log.info("Sleeping for 2 seconds...");
    Thread.sleep(2000);

    for (int i = 0; i < NUM_CLIENTS; i++)
    {
      testWriters[i].running = false;
      testWriterFutures[i].join();
      log.info("Joined TestWriter {}", testWriters[i].user);
    }


    log.info("Sleeping for 2 seconds...");
    Thread.sleep(2000);
    log.info("Joining TestListener...");
    testListener.running = false;
    testListenerFuture.join();
    log.info("Joined TestListener");
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
