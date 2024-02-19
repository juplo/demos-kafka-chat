package de.juplo.kafka.chat.backend.storage.nostorage;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.UUID;


@Slf4j
public class NoStorageStorageStrategy implements StorageStrategy
{
  @Override
  public void write(ChatHomeService chatHomeService)
  {
    log.info("Storage is disabled: Not storing {}", chatHomeService);
  }

  @Override
  public void writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux) {}

  @Override
  public Flux<ChatRoomInfo> readChatRoomInfo()
  {
    return Flux.empty();
  }

  @Override
  public void writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux) {}

  @Override
  public Flux<Message> readChatRoomData(UUID chatRoomId)
  {
    return Flux.empty();
  }
}
