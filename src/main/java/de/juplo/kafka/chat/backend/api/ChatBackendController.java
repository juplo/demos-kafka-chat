package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
public class ChatBackendController
{
  private final ChatHome chatHome;
  private final StorageStrategy storageStrategy;


  @PostMapping("create")
  public Mono<ChatRoomTo> create(@RequestBody String name)
  {
    return chatHome.createChatroom(name).map(ChatRoomTo::from);
  }

  @GetMapping("list")
  public Flux<ChatRoomTo> list()
  {
    return chatHome.getChatRooms().map(chatroom -> ChatRoomTo.from(chatroom));
  }

  @GetMapping("list/{chatroomId}")
  public Flux<MessageTo> list(@PathVariable UUID chatroomId)
  {
    return chatHome
        .getChatRoom(chatroomId)
        .flatMapMany(chatroom -> chatroom
            .getMessages()
            .map(MessageTo::from));
  }

  @GetMapping("get/{chatroomId}")
  public Mono<ChatRoomTo> get(@PathVariable UUID chatroomId)
  {
    return chatHome.getChatRoom(chatroomId).map(chatroom -> ChatRoomTo.from(chatroom));
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
            .getChatRoom(chatroomId)
            .flatMap(chatroom -> put(chatroom, username, messageId, text));
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
            .getChatRoom(chatroomId)
            .flatMap(chatroom -> get(chatroom, username, messageId));
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
        .getChatRoom(chatroomId)
        .flatMapMany(chatroom -> listen(chatroom));
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
    storageStrategy.writeChatrooms(chatHome.getChatRooms());
  }
}
