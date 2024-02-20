package de.juplo.kafka.chat.backend.storage.nostorage;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


@Slf4j
public class NoStorageStorageStrategy implements StorageStrategy
{
  public Mono<Void> write(ChatHomeService chatHomeService)
  {
    return Mono
        .<Void>empty()
        .doOnSuccess(empty -> log.info("Storage is disabled: Not storing {}", chatHomeService));

  }

  public Flux<ChatRoomInfo> writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux)
  {
    return chatRoomInfoFlux;
  }

  @Override
  public Flux<ChatRoomInfo> readChatRoomInfo()
  {
    return Flux.empty();
  }

  @Override
  public Flux<Message> writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux)
  {
    return messageFlux;
  }

  @Override
  public Flux<Message> readChatRoomData(UUID chatRoomId)
  {
    return Flux.empty();
  }
}
