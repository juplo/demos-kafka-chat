package de.juplo.kafka.chatroom.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;


@RequiredArgsConstructor
@Slf4j
public class Chatroom
{
  @Getter
  private final UUID id;
  @Getter
  private final String name;
  private final List<Message> messages = new LinkedList<>();
  private final Sinks.Many<Message> sink = Sinks.many().multicast().onBackpressureBuffer();

  synchronized public Mono<Message> addMessage(
      UUID id,
      LocalDateTime timestamp,
      String user,
      String text)
  {
    return persistMessage(id, timestamp, user, text)
        .doOnNext(message -> sink.tryEmitNext(message).orThrow());
  }

  private Mono<Message> persistMessage(
      UUID id,
      LocalDateTime timestamp,
      String user,
      String text)
  {
    Message message = new Message(id, (long)messages.size(), timestamp, user, text);
    messages.add(message);
    return Mono
        .fromSupplier(() -> message)
        .log();
  }

  public Flux<Message> listen()
  {
    return sink.asFlux();
  }

  public Stream<Message> getMessages(long firstMessage)
  {
    return messages.stream().filter(message -> message.getSerialNumber() >= firstMessage);
  }
}
