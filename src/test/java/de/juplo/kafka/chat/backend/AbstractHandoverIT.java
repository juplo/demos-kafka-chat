package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import pl.rzrz.assertj.reactor.Assertions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;


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
    log.info("Starting backend-1...");
    containers.startBackend(containers.backend1, new TestWriter[0]);
    log.info("backend-1 started!");

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
    testListener
        .run()
        .subscribe(message -> log.info(
            "Received message: {}",
            message));

    log.info("Starting backend-2...");
    containers.startBackend(containers.backend2, testWriters);
    log.info("backend-2 started!");

    log.info("Starting backend-3...");
    containers.startBackend(containers.backend3, testWriters);
    log.info("backend-3 started!");

    for (int i = 0; i < NUM_CLIENTS; i++)
    {
      testWriters[i].running = false;
      testWriterFutures[i].join();
      log.info("Joined TestWriter {}", testWriters[i].user);
    }

    Awaitility
        .await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertAllSentMessagesReceived(testWriters, testListener));
  }

  private void assertAllSentMessagesReceived(
      TestWriter[] testWriters,
      TestListener testListener)
  {
    for (int i = 0; i < NUM_CLIENTS; i++)
    {
      TestWriter testWriter = testWriters[i];
      ChatRoomInfoTo chatRoom = testWriter.chatRoom;
      List<MessageTo> receivedMessages = testListener.receivedMessages.get(chatRoom.getId());

      Assertions.assertThat(receivedMessages
          .stream()
          .filter(message -> message.getUser().equals(testWriter.user.getName()))
          ).containsExactlyElementsOf(testWriter.sentMessages);
    }
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
