package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomData;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
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
  private final ChatHomeService chatHomeService;
  private final StorageStrategy storageStrategy;


  @PostMapping("create")
  public Mono<ChatRoomInfoTo> create(@RequestBody String name)
  {
    UUID chatRoomId = UUID.randomUUID();
    return chatHomeService
        .createChatRoom(chatRoomId, name)
        .map(ChatRoomInfoTo::from);
  }

  @GetMapping("list")
  public Flux<ChatRoomInfoTo> list()
  {
    return chatHomeService
        .getChatRoomInfo()
        .map(chatroomInfo -> ChatRoomInfoTo.from(chatroomInfo));
  }

  @GetMapping("{chatRoomId}/list")
  public Flux<MessageTo> list(@PathVariable UUID chatRoomId)
  {
    return chatHomeService
        .getChatRoomData(chatRoomId)
        .flatMapMany(chatRoomData -> chatRoomData
            .getMessages()
            .map(MessageTo::from));
  }

  @GetMapping("{chatRoomId}")
  public Mono<ChatRoomInfoTo> get(@PathVariable UUID chatRoomId)
  {
    return chatHomeService
        .getChatRoomInfo(chatRoomId)
        .map(chatRoomInfo -> ChatRoomInfoTo.from(chatRoomInfo));
  }

  @PutMapping("{chatRoomId}/{username}/{messageId}")
  public Mono<MessageTo> put(
      @PathVariable UUID chatRoomId,
      @PathVariable String username,
      @PathVariable Long messageId,
      @RequestBody String text)
  {
    return
        chatHomeService
            .getChatRoomData(chatRoomId)
            .flatMap(chatRoomData -> put(chatRoomData, username, messageId, text));
  }

  private Mono<MessageTo> put(
      ChatRoomData chatRoomData,
      String username,
      Long messageId,
      String text)
  {
    return
        chatRoomData
            .addMessage(
                messageId,
                username,
                text)
            .map(message -> MessageTo.from(message));
  }

  @GetMapping("{chatRoomId}/{username}/{messageId}")
  public Mono<MessageTo> get(
      @PathVariable UUID chatRoomId,
      @PathVariable String username,
      @PathVariable Long messageId)
  {
    return
        chatHomeService
            .getChatRoomData(chatRoomId)
            .flatMap(chatRoomData -> get(chatRoomData, username, messageId));
  }

  private Mono<MessageTo> get(
      ChatRoomData chatRoomData,
      String username,
      Long messageId)
  {
    return
        chatRoomData
            .getMessage(username, messageId)
            .map(message -> MessageTo.from(message));
  }

  @GetMapping(path = "{chatRoomId}/listen")
  public Flux<ServerSentEvent<MessageTo>> listen(@PathVariable UUID chatRoomId)
  {
    return chatHomeService
        .getChatRoomData(chatRoomId)
        .flatMapMany(chatRoomData -> listen(chatRoomData));
  }

  private Flux<ServerSentEvent<MessageTo>> listen(ChatRoomData chatRoomData)
  {
    return chatRoomData
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

  @GetMapping("/shards")
  public Mono<String[]> getShardOwners()
  {
    return chatHomeService.getShardOwners();
  }

  @PostMapping("/store")
  public void store()
  {
    storageStrategy
        .write(chatHomeService)
        .subscribe();
  }
}
