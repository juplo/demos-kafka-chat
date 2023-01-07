package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.Chatroom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
public class ChatBackendController
{
  private final ChatHome chatHome;
  private final Clock clock;


  @PostMapping("create")
  public Chatroom create(@RequestBody String name)
  {
    return chatHome.createChatroom(name);
  }

  @GetMapping("list")
  public Collection<Chatroom> list()
  {
    return chatHome.list();
  }

  @GetMapping("get/{chatroomId}")
  public Optional<Chatroom> get(@PathVariable UUID chatroomId)
  {
    return chatHome.getChatroom(chatroomId);
  }

  @PutMapping("put/{chatroomId}/{username}/{messageId}")
  public Mono<MessageTo> put(
      @PathVariable UUID chatroomId,
      @PathVariable String username,
      @PathVariable Long messageId,
      @RequestBody String text)
  {
    return
        chatHome
            .getChatroom(chatroomId)
            .map(chatroom -> put(chatroom, username, messageId, text))
            .orElseThrow(() -> new UnknownChatroomException(chatroomId));
  }

  public Mono<MessageTo> put(
      Chatroom chatroom,
      String username,
      Long messageId,
      String text)
  {
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
        chatHome
            .getChatroom(chatroomId)
            .map(chatroom -> get(chatroom, username, messageId))
            .orElseThrow(() -> new UnknownChatroomException(chatroomId));
  }

  private Mono<MessageTo> get(
      Chatroom chatroom,
      String username,
      Long messageId)
  {
    return
        chatroom
            .getMessage(username, messageId)
            .map(message -> MessageTo.from(message));
  }

  @GetMapping(
      path = "listen/{chatroomId}",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<MessageTo> listen(@PathVariable UUID chatroomId)
  {
    return chatHome
        .getChatroom(chatroomId)
        .map(chatroom -> listen(chatroom))
        .orElseThrow(() -> new UnknownChatroomException(chatroomId));
  }

  private Flux<MessageTo> listen(Chatroom chatroom)
  {
    return chatroom
        .listen()
        .log()
        .map(message -> MessageTo.from(message));
  }
}
