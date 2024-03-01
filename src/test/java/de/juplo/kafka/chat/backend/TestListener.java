package de.juplo.kafka.chat.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
public class TestListener implements Runnable
{
  static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<>() {};


  @Override
  public void run()
  {
    Flux
        .fromArray(chatRooms)
        .flatMap(chatRoom -> receiveMessages(chatRoom)
            .flatMap(sse ->
            {
              try
              {
                return Mono.just(objectMapper.readValue(sse.data(), MessageTo.class));
              }
              catch (Exception e)
              {
                return Mono.error(e);
              }
            })
            .doOnNext(message -> log.info(
                "Received a message from chat-room {}: {}",
                chatRoom,
                message))
            .limitRate(10))
        .takeUntil(message -> !running)
        .then()
        .block();
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


  private final WebClient webClient;
  private final ChatRoomInfoTo[] chatRooms;
  private final ObjectMapper objectMapper;

  volatile boolean running = true;


  TestListener(Integer port, ChatRoomInfoTo[] chatRooms)
  {
    webClient = WebClient.create("http://localhost:" + port);
    this.chatRooms = chatRooms;
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }
}
