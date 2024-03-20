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


@Testcontainers
@Slf4j
public abstract class AbstractHandoverIT
{
  static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<>() {};


  private final AbstractHandoverITContainers containers;


  AbstractHandoverIT(AbstractHandoverITContainers containers)
  {
    this.containers = containers;
  }


  @Test
  void test() throws InterruptedException
  {
    ChatRoomInfoTo chatRoom = createChatRoom("bar").block();
    TestClient testClient = new TestClient(
        containers.haproxy.getMappedPort(8400),
        chatRoom,
        "nerd");
    testClient.run();

    receiveMessages(chatRoom)
        .take(100)
        .doOnNext(message -> log.info("message: {}", message))
        .then()
        .block();
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

  Flux<ServerSentEvent<String>> receiveMessages(ChatRoomInfoTo chatRoom)
  {
    return webClient
        .get()
        .uri(
            "/{chatRoomId}/listen",
            chatRoom.getId())
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(SSE_TYPE);
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
