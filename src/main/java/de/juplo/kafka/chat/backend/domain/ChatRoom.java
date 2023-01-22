package de.juplo.kafka.chat.backend.domain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.SynchronousSink;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class ChatRoom extends ChatRoomInfo
{
  public final static Pattern VALID_USER = Pattern.compile("^[a-z0-9-]{2,}$");
  private final Clock clock;
  private final ChatRoomService service;
  private final int bufferSize;
  private Sinks.Many<Message> sink;


  public ChatRoom(
      UUID id,
      String name,
      int shard,
      Clock clock,
      ChatRoomService service,
      int bufferSize)
  {
    super(id, name, shard);
    log.info("Created ChatRoom {} with buffer-size {}", id, bufferSize);
    this.clock = clock;
    this.service = service;
    this.bufferSize = bufferSize;
    // @RequiredArgsConstructor unfortunately not possible, because
    // the `bufferSize` is not set, if `createSink()` is called
    // from the variable declaration!
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
        .handle((Message existing, SynchronousSink<Message> sink) ->
        {
          if (existing.getMessageText().equals(text))
          {
            sink.next(existing);
          }
          else
          {
            sink.error(new MessageMutationException(existing, text));
          }
        })
        .switchIfEmpty(
            Mono
                .defer(() -> service.persistMessage(key, LocalDateTime.now(clock), text))
                .doOnNext(m ->
                {
                  Sinks.EmitResult result = sink.tryEmitNext(m);
                  if (result.isFailure())
                  {
                    log.warn("Emitting of message failed with {} for {}", result.name(), m);
                  }
                }));
  }


  public ChatRoomService getChatRoomService()
  {
    return service;
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
