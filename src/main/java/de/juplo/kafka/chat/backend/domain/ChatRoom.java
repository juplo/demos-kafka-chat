package de.juplo.kafka.chat.backend.domain;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.*;


@Slf4j
public class ChatRoom
{
  @Getter
  private final UUID id;
  @Getter
  private final String name;
  private final ChatroomService chatroomService;
  private final int bufferSize;
  private Sinks.Many<Message> sink;

  public ChatRoom(
      UUID id,
      String name,
      ChatroomService chatroomService,
      int bufferSize)
  {
    this.id = id;
    this.name = name;
    this.chatroomService = chatroomService;
    this.bufferSize = bufferSize;
    this.sink = createSink();
  }


  synchronized public Mono<Message> addMessage(
      Long id,
      LocalDateTime timestamp,
      String user,
      String text)
  {
    return chatroomService
        .persistMessage(Message.MessageKey.of(user, id), timestamp, text)
        .doOnNext(message ->
        {
          Sinks.EmitResult result = sink.tryEmitNext(message);
          if (result.isFailure())
          {
            log.warn("Emitting of message failed with {} for {}", result.name(), message);
          }
        });
  }


  public Mono<Message> getMessage(String username, Long messageId)
  {
    Message.MessageKey key = Message.MessageKey.of(username, messageId);
    return chatroomService.getMessage(key);
  }

  synchronized public Flux<Message> listen()
  {
    return sink
        .asFlux()
        .doOnCancel(() -> sink = createSink()); // Sink hast to be recreated on auto-cancel!
  }

  public Flux<Message> getMessages()
  {
    return getMessages(0, Long.MAX_VALUE);
  }

  public Flux<Message> getMessages(long first, long last)
  {
    return chatroomService.getMessages(first, last);
  }

  private Sinks.Many<Message> createSink()
  {
    return Sinks
        .many()
        .multicast()
        .onBackpressureBuffer(bufferSize);
  }
}
