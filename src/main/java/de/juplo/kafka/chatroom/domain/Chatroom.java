package de.juplo.kafka.chatroom.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;


@RequiredArgsConstructor
@Slf4j
public class Chatroom
{
  @Getter
  private final UUID id;
  @Getter
  private final String name;
  private final LinkedHashMap<MessageKey, Message> messages = new LinkedHashMap<>();
  private final Sinks.Many<Message> sink = Sinks.many().multicast().onBackpressureBuffer();

  synchronized public Mono<Message> addMessage(
      Long id,
      LocalDateTime timestamp,
      String user,
      String text)
  {
    return persistMessage(id, timestamp, user, text)
        .doOnNext(message -> sink.tryEmitNext(message).orThrow());
  }

  private Mono<Message> persistMessage(
      Long id,
      LocalDateTime timestamp,
      String user,
      String text)
  {
    Message message = new Message(id, (long)messages.size(), timestamp, user, text);

    MessageKey key = new MessageKey(user, id);
    Message existing = messages.get(key);
    if (existing != null)
    {
      log.info("Message with key {} already exists; {}", key, existing);
      if (!message.equals(existing))
        throw new IllegalArgumentException("Messages are imutable!");
      return Mono.empty();
    }

    messages.put(key, message);
    return Mono
        .fromSupplier(() -> message)
        .log();
  }

  public Mono<Message> getMessage(String username, Long messageId)
  {
    return Mono.fromSupplier(() ->
    {
      MessageKey key = MessageKey.of(username, messageId);
      return messages.get(key);
    });
  }

  public Flux<Message> listen()
  {
    return sink.asFlux();
  }

  public Stream<Message> getMessages(long firstMessage)
  {
    return messages
        .values()
        .stream()
        .filter(message -> message.getSerialNumber() >= firstMessage);
  }


  @Value(staticConstructor = "of")
  static class MessageKey
  {
    String username;
    Long messageId;
  }
}
