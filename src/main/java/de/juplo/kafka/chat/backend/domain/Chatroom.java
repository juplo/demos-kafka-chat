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
  private final Sinks.Many<Message> sink;

  public Chatroom(
      UUID id,
      String name,
      PersistenceStrategy persistence,
      int bufferSize)
  {
    this.id = id;
    this.name = name;
    this.persistence = persistence;
    this.sink = Sinks.many().multicast().onBackpressureBuffer(bufferSize);
  }


  synchronized public Mono<Message> addMessage(
      Long id,
      LocalDateTime timestamp,
      String user,
      String text)
  {
    return persistence
        .persistMessage(Message.MessageKey.of(user, id), timestamp, text)
        .doOnNext(message -> sink.tryEmitNext(message).orThrow());
  }


  public Mono<Message> getMessage(String username, Long messageId)
  {
    return persistence.getMessage(Message.MessageKey.of(username, messageId));
  }

  public Flux<Message> listen()
  {
    return sink.asFlux();
  }

  public Flux<Message> getMessages()
  {
    return getMessages(0, Long.MAX_VALUE);
  }

  public Flux<Message> getMessages(long first, long last)
  {
    return persistence.getMessages(first, last);
  }
}
