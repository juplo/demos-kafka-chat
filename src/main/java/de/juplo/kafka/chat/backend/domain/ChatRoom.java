package de.juplo.kafka.chat.backend.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@EqualsAndHashCode(of = { "id" })
@ToString(of = { "id", "name" })
public class ChatRoom
{
  public final static Pattern VALID_USER = Pattern.compile("^[a-z0-9-]{2,}$");
  @Getter
  private final UUID id;
  @Getter
  private final String name;
  private final Clock clock;
  private final ChatRoomService service;
  private final int bufferSize;
  private Sinks.Many<Message> sink;

  public ChatRoom(
      UUID id,
      String name,
      Clock clock,
      ChatRoomService service,
      int bufferSize)
  {
    this.id = id;
    this.name = name;
    this.clock = clock;
    this.service = service;
    this.bufferSize = bufferSize;
    this.sink = createSink();
  }


  synchronized public Mono<Message> addMessage(
      Long id,
      String user,
      String text)
  {
    Matcher matcher = VALID_USER.matcher(user);
    if (!matcher.matches())
      throw new InvalidUsernameException(user);

    Message.MessageKey key = Message.MessageKey.of(user, id);
    return service
        .getMessage(key)
        .flatMap(existing -> text.equals(existing.getMessageText())
            ? Mono.just(existing)
            : Mono.error(() -> new MessageMutationException(existing, text)))
        .switchIfEmpty(
            Mono
                .fromSupplier(() ->service.persistMessage(key, LocalDateTime.now(clock), text))
                .doOnNext(m ->
                {
                  Sinks.EmitResult result = sink.tryEmitNext(m);
                  if (result.isFailure())
                  {
                    log.warn("Emitting of message failed with {} for {}", result.name(), m);
                  }
                }));
  }


  public Mono<Message> getMessage(String username, Long messageId)
  {
    Message.MessageKey key = Message.MessageKey.of(username, messageId);
    return service.getMessage(key);
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
    return service.getMessages(first, last);
  }

  private Sinks.Many<Message> createSink()
  {
    return Sinks
        .many()
        .multicast()
        .onBackpressureBuffer(bufferSize);
  }
}
