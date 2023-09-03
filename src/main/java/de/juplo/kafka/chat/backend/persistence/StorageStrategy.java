package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import reactor.core.publisher.Flux;

import java.util.UUID;


public interface StorageStrategy
{
  default void write(ChatHome chatHome)
  {
    writeChatRoomInfo(
        chatHome
            .getChatRoomInfo()
            .doOnNext(chatRoomInfo -> writeChatRoomData(
                chatRoomInfo.getId(),
                chatHome
                    .getChatRoomData(chatRoomInfo.getId())
                    .flatMapMany(chatRoomData -> chatRoomData.getMessages()))));
  }

  void writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux);
  Flux<ChatRoomInfo> readChatRoomInfo();
  void writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux);
  Flux<Message> readChatRoomData(UUID chatRoomId);
}
