package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.domain.*;
import de.juplo.kafka.chat.backend.domain.exceptions.UnknownChatroomException;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;


@Slf4j
public class SimpleChatHomeService implements ChatHomeService
{
  private final Integer shard;
  private final Map<UUID, ChatRoomInfo> chatRoomInfo;
  private final Map<UUID, ChatRoomData> chatRoomData;
  private final Clock clock;
  private final int bufferSize;



  public SimpleChatHomeService(
      Clock clock,
      int bufferSize)
  {
    this(
        null,
        clock,
        bufferSize);
  }

  public SimpleChatHomeService(
      Integer shard,
      Clock clock,
      int bufferSize)
  {
    log.debug("Creating SimpleChatHomeService");

    this.shard = shard;
    this.chatRoomInfo = new HashMap<>();
    this.chatRoomData = new HashMap<>();
    this.clock = clock;
    this.bufferSize = bufferSize;
  }


  Mono<Void> restore(StorageStrategy storageStrategy)
  {
    chatRoomInfo.clear();
    chatRoomData.clear();

    return storageStrategy
        .readChatRoomInfo()
        .filter(info ->
        {
          if (shard == null || info.getShard() == shard)
          {
            return true;
          }
          else
          {
            log.info(
                "SimpleChatHome for shard {} ignores not owned chat-room {}",
                shard,
                info);
            return false;
          }
        })
        .flatMap(info ->
        {
          UUID chatRoomId = info.getId();
          InMemoryChatMessageService chatMessageService =
              new InMemoryChatMessageService(chatRoomId);

          chatRoomInfo.put(chatRoomId, info);
          chatRoomData.put(
              info.getId(),
              new ChatRoomData(
                  clock,
                  chatMessageService,
                  bufferSize));

          return chatMessageService.restore(storageStrategy);
        })
        .then()
        .doOnSuccess(empty -> log.info("Restored {}", this))
        .doOnError(throwable -> log.error("Could not restore {}", this));
  }


  @Override
  public Mono<ChatRoomInfo> createChatRoom(UUID id, String name)
  {
    log.info("Creating ChatRoom with buffer-size {}", bufferSize);
    ChatMessageService service = new InMemoryChatMessageService(id);
    ChatRoomInfo chatRoomInfo = new ChatRoomInfo(id, name, shard);
    this.chatRoomInfo.put(id, chatRoomInfo);
    ChatRoomData chatRoomData = new ChatRoomData(clock, service, bufferSize);
    this.chatRoomData.put(id, chatRoomData);
    return Mono.just(chatRoomInfo);
  }

  @Override
  public Mono<ChatRoomInfo> getChatRoomInfo(UUID id)
  {
    return Mono
        .justOrEmpty(chatRoomInfo.get(id))
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(id)));
  }

  @Override
  public Flux<ChatRoomInfo> getChatRoomInfo()
  {
    return Flux.fromIterable(chatRoomInfo.values());
  }

  @Override
  public Mono<ChatRoomData> getChatRoomData(UUID id)
  {
    return Mono
        .justOrEmpty(chatRoomData.get(id))
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(id)));
  }

  @Override
  public Mono<String[]> getShardOwners()
  {
    return Mono.empty();
  }

  @Override
  public String toString()
  {
    return SimpleChatHomeService.class.getSimpleName() + ", shard=" + shard;
  }
}
