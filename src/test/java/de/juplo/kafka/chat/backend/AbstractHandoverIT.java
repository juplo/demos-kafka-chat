package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.stream.IntStream;


@Testcontainers
@Slf4j
public abstract class AbstractHandoverIT
{
  static final ImagePullPolicy NEVER_PULL = imageName -> false;
  static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<>() {};


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

  Mono<MessageTo> sendMessage(
      ChatRoomInfoTo chatRoom,
      User user,
      String message)
  {
    return webClient
        .put()
        .uri(
            "/{chatRoomId}/{username}/{serial}",
            chatRoom.getId(),
            user.getName(),
            user.nextSerial())
        .contentType(MediaType.TEXT_PLAIN)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(message)
        .exchangeToMono(response ->
        {
          if (response.statusCode().equals(HttpStatus.OK))
          {
            return response.bodyToMono(MessageTo.class);
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
