package de.juplo.kafka.chat.backend.storage.nostorage;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import reactor.core.publisher.Flux;

import java.util.UUID;


public class NoStorageStorageStrategy implements StorageStrategy
{
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
