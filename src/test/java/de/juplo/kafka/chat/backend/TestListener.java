package de.juplo.kafka.chat.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@Slf4j
public class TestListener
{
  static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<>() {};


  public Flux<MessageTo> run()
  {
    return Flux
        .fromArray(chatRooms)
        .flatMap(chatRoom ->
        {
          List<MessageTo> list = new LinkedList<>();
          receivedMessages.put(chatRoom.getId(), list);
          return receiveMessages(chatRoom);
        });
  }

  Flux<MessageTo> receiveMessages(ChatRoomInfoTo chatRoom)
  {
    log.info("Requesting messages for chat-room {}", chatRoom);
    List<MessageTo> list = receivedMessages.get(chatRoom.getId());
    return receiveServerSentEvents(chatRoom)
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
        .doOnNext(message -> list.add(message))
        .doOnComplete(() -> log.info("Listening to {} was completed!", chatRoom))
        .doOnError(throwalbe -> log.error("Listening to {} failed!", chatRoom, throwalbe));
  }

  Flux<ServerSentEvent<String>> receiveServerSentEvents(ChatRoomInfoTo chatRoom)
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

  final Map<UUID, List<MessageTo>> receivedMessages = new HashMap<>();


  TestListener(Integer port, ChatRoomInfoTo[] chatRooms)
  {
    webClient = WebClient.create("http://localhost:" + port);
    this.chatRooms = chatRooms;
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }
}
