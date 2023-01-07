package de.juplo.kafka.chat.backend.domain;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.*;


@Slf4j
public class Chatroom
{
  @Getter
  private final UUID id;
  @Getter
  private final String name;
  private final PersistenceStrategy persistence;
  private final int bufferSize;
  private Sinks.Many<Message> sink;

  public Chatroom(
      UUID id,
      String name,
      PersistenceStrategy persistence,
      int bufferSize)
  {
    this.id = id;
    this.name = name;
    this.persistence = persistence;
    this.bufferSize = bufferSize;
    this.sink = createSink();
  }


  synchronized public Mono<Message> addMessage(
      Long id,
      LocalDateTime timestamp,
      String user,
      String text)
  {
    return persistence
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
    return persistence.getMessage(Message.MessageKey.of(username, messageId));
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
    return persistence.getMessages(first, last);
  }

  private Sinks.Many<Message> createSink()
  {
    return Sinks
        .many()
        .multicast()
        .onBackpressureBuffer(bufferSize);
  }
}
