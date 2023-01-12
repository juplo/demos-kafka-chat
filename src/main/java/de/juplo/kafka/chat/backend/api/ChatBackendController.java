package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatRoomFactory;
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
  private final ChatHome[] chatHomes;
  private final ShardingStrategy selectionStrategy;
  private final ChatRoomFactory factory;
  private final StorageStrategy storageStrategy;


  @PostMapping("create")
  public Mono<ChatRoomTo> create(@RequestBody String name)
  {
    UUID chatRoomId = UUID.randomUUID();
    return factory
        .createChatRoom(chatRoomId, name)
        .flatMap(chatRoom -> chatHomes[chatRoom.getShard()].putChatRoom(chatRoom))
        .map(ChatRoomTo::from);
  }

  @GetMapping("list")
  public Flux<ChatRoomTo> list()
  {
    return Flux
        .fromArray(chatHomes)
        .flatMap(chatHome -> chatHome.getChatRooms())
        .map(chatroom -> ChatRoomTo.from(chatroom));
  }

  @GetMapping("{chatroomId}/list")
  public Flux<MessageTo> list(@PathVariable UUID chatroomId)
  {
    int shard = selectionStrategy.selectShard(chatroomId);
    return chatHomes[shard]
        .getChatRoom(chatroomId)
        .flatMapMany(chatroom -> chatroom
            .getMessages()
            .map(MessageTo::from));
  }

  @GetMapping("{chatroomId}")
  public Mono<ChatRoomTo> get(@PathVariable UUID chatroomId)
  {
    int shard = selectionStrategy.selectShard(chatroomId);
    return chatHomes[shard]
        .getChatRoom(chatroomId)
        .map(chatroom -> ChatRoomTo.from(chatroom));
  }

  @PutMapping("{chatroomId}/{username}/{messageId}")
  public Mono<MessageTo> put(
      @PathVariable UUID chatroomId,
      @PathVariable String username,
      @PathVariable Long messageId,
      @RequestBody String text)
  {
    int shard = selectionStrategy.selectShard(chatroomId);
    return
        chatHomes[shard]
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

  @GetMapping("{chatroomId}/{username}/{messageId}")
  public Mono<MessageTo> get(
      @PathVariable UUID chatroomId,
      @PathVariable String username,
      @PathVariable Long messageId)
  {
    int shard = selectionStrategy.selectShard(chatroomId);
    return
        chatHomes[shard]
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

  @GetMapping(path = "{chatroomId}/listen")
  public Flux<ServerSentEvent<MessageTo>> listen(@PathVariable UUID chatroomId)
  {
    int shard = selectionStrategy.selectShard(chatroomId);
    return chatHomes[shard]
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
    for (int shard = 0; shard < chatHomes.length; shard++)
      storageStrategy.write(chatHomes[shard].getChatRooms());
  }
}
