package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


@RestController
@RequiredArgsConstructor
public class ChatBackendController
{
  private final ChatHome chatHome;
  private final StorageStrategy storageStrategy;


  @PostMapping("create")
  public ChatRoomTo create(@RequestBody String name)
  {
    return ChatRoomTo.from(chatHome.createChatroom(name));
  }

  @GetMapping("list")
  public Stream<ChatRoomTo> list()
  {
    return chatHome.list().map(chatroom -> ChatRoomTo.from(chatroom));
  }

  @GetMapping("list/{chatroomId}")
  public Flux<MessageTo> list(@PathVariable UUID chatroomId)
  {
    return chatHome
        .getChatroom(chatroomId)
        .map(chatroom -> chatroom
            .getMessages()
            .map(MessageTo::from))
        .get();
  }

  @GetMapping("get/{chatroomId}")
  public Optional<ChatRoomTo> get(@PathVariable UUID chatroomId)
  {
    return chatHome.getChatroom(chatroomId).map(chatroom -> ChatRoomTo.from(chatroom));
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
      ChatRoom chatroom,
      String username,
      Long messageId,
      String text)
  {
    return
        chatroom
            .addMessage(
                messageId,
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
      ChatRoom chatroom,
      String username,
      Long messageId)
  {
    return
        chatroom
            .getMessage(username, messageId)
            .map(message -> MessageTo.from(message));
  }

  @GetMapping(path = "listen/{chatroomId}")
  public Flux<ServerSentEvent<MessageTo>> listen(@PathVariable UUID chatroomId)
  {
    return chatHome
        .getChatroom(chatroomId)
        .map(chatroom -> listen(chatroom))
        .orElseThrow(() -> new UnknownChatroomException(chatroomId));
  }

  private Flux<ServerSentEvent<MessageTo>> listen(ChatRoom chatroom)
  {
    return chatroom
        .listen()
        .log()
        .map(message -> MessageTo.from(message))
        .map(messageTo ->
            ServerSentEvent
                .builder(messageTo)
                .id(messageTo.getSerial().toString())
                .event("message")
                .build());
  }

  @PostMapping("/store")
  public void store()
  {
    storageStrategy.writeChatrooms(Flux.fromStream(chatHome.list()));
  }
}
