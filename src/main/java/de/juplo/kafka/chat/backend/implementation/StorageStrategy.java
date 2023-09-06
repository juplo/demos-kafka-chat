package de.juplo.kafka.chat.backend.implementation;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import reactor.core.publisher.Flux;

import java.util.UUID;


public interface StorageStrategy
{
  default void write(ChatHomeService chatHomeService)
  {
    writeChatRoomInfo(
        chatHomeService
            .getChatRoomInfo()
            .doOnNext(chatRoomInfo -> writeChatRoomData(
                chatRoomInfo.getId(),
                chatHomeService
                    .getChatRoomData(chatRoomInfo.getId())
                    .flatMapMany(chatRoomData -> chatRoomData.getMessages()))));
  }

  void writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux);
  Flux<ChatRoomInfo> readChatRoomInfo();
  void writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux);
  Flux<Message> readChatRoomData(UUID chatRoomId);
}
