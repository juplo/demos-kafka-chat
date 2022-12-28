package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.Chatroom;
import de.juplo.kafka.chat.backend.domain.PersistenceStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
public class ChatBackendController
{
  private final Map<UUID, Chatroom> chatrooms = new HashMap<>();
  private final PersistenceStrategy persistenceStrategy;
  private final Clock clock;


  @PostMapping("create")
  public Chatroom create(@RequestBody String name)
  {
    Chatroom chatroom = new Chatroom(UUID.randomUUID(), name, persistenceStrategy);
    chatrooms.put(chatroom.getId(), chatroom);
    return chatroom;
  }

  @GetMapping("list")
  public Collection<Chatroom> list()
  {
    return chatrooms.values();
  }

  @GetMapping("get/{chatroomId}")
  public Chatroom get(@PathVariable UUID chatroomId)
  {
    return chatrooms.get(chatroomId);
  }

  @PutMapping("put/{chatroomId}/{username}/{messageId}")
  public Mono<MessageTo> put(
      @PathVariable UUID chatroomId,
      @PathVariable String username,
      @PathVariable Long messageId,
      @RequestBody String text)
  {
    Chatroom chatroom = chatrooms.get(chatroomId);
    return
        chatroom
            .addMessage(
                messageId,
                LocalDateTime.now(clock),
                username,
                text)
            .switchIfEmpty(chatroom.getMessage(username, messageId))
            .map(message -> MessageTo.from(message));
  }

  @GetMapping("get/{chatroomId}/{username}/{messageId}")
  public Mono<MessageTo> get(
      @PathVariable UUID chatroomId,
      @PathVariable String username,
      @PathVariable Long messageId)
  {
    return
        chatrooms
            .get(chatroomId)
            .getMessage(username, messageId)
            .map(message -> MessageTo.from(message));
  }

  @GetMapping(
      path = "listen/{chatroomId}",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<MessageTo> listen(@PathVariable UUID chatroomId)
  {
    return chatrooms
        .get(chatroomId)
        .listen()
        .log()
        .map(message -> MessageTo.from(message));
  }
}
