package de.juplo.kafka.chat.backend.domain;

import de.juplo.kafka.chat.backend.domain.exceptions.ChatRoomInactiveException;
import de.juplo.kafka.chat.backend.domain.exceptions.InvalidUsernameException;
import de.juplo.kafka.chat.backend.domain.exceptions.MessageMutationException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.SynchronousSink;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class ChatRoomData
{
  public final static Pattern VALID_USER = Pattern.compile("^[a-z0-9-]{2,}$");

  private final ChatMessageService service;
  private final Clock clock;
  private final int historyLimit;
  private Sinks.Many<Message> sink;
  private volatile boolean active = false;


  public ChatRoomData(
      Clock clock,
      ChatMessageService service,
      int historyLimit)
  {
    log.info("Created ChatRoom with history-limit {}", historyLimit);
    this.clock = clock;
    this.service = service;
    this.historyLimit = historyLimit;
    // @RequiredArgsConstructor unfortunately not possible, because
    // the `historyLimit` is not set, if `createSink()` is called
    // from the variable declaration!
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
        .switchIfEmpty(active
            ? Mono
                .defer(() -> service.persistMessage(key, LocalDateTime.now(clock), text))
                .doOnNext(m ->
                {
                  Sinks.EmitResult result = sink.tryEmitNext(m);
                  if (result.isFailure())
                  {
                    log.warn("Emitting of message failed with {} for {}", result.name(), m);
                  }
                })
            : Mono.error(new ChatRoomInactiveException(service.getChatRoomInfo())));
  }


  public ChatMessageService getChatRoomService()
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
    return active
        ? sink
            .asFlux()
            .doOnCancel(() -> sink = createSink()) // Sink hast to be recreated on auto-cancel!
        : Flux
            .error(new ChatRoomInactiveException(service.getChatRoomInfo()));

  }

  public Flux<Message> getMessages()
  {
    return getMessages(0, Long.MAX_VALUE);
  }

  public Flux<Message> getMessages(long first, long last)
  {
    return service.getMessages(first, last);
  }

  public void activate()
  {
    if (active)
    {
      log.info("{} is already active!", service.getChatRoomInfo());
      return;
    }

    log.info("{} is being activated", service.getChatRoomInfo());
    this.sink = createSink();
    active = true;
  }

  public void deactivate()
  {
    log.info("{} is being deactivated", service.getChatRoomInfo());
    active = false;
    sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
  }

  private Sinks.Many<Message> createSink()
  {
    return Sinks
        .many()
        .replay()
        .limit(historyLimit);
  }
}
