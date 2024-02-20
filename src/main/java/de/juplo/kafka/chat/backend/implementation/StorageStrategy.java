package de.juplo.kafka.chat.backend.implementation;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.UUID;


public interface StorageStrategy
{
  Logger log = LoggerFactory.getLogger(StorageStrategy.class.getCanonicalName());

  default Flux<ChatRoomInfo> write(ChatHomeService chatHomeService)
  {
    return writeChatRoomInfo(
        chatHomeService
            .getChatRoomInfo()
            .doOnComplete(() -> log.info("Stored {}", chatHomeService))
            .doOnError(throwable -> log.error("Could not store {}: {}", chatHomeService, throwable))
            .doOnNext(chatRoomInfo -> writeChatRoomData(
                chatRoomInfo.getId(),
                chatHomeService
                    .getChatRoomData(chatRoomInfo.getId())
                    .flatMapMany(chatRoomData -> chatRoomData.getMessages())
                    .doOnComplete(() -> log.info("Stored {}", chatRoomInfo))
                    .doOnError(throwable -> log.error("Could not store {}: {}", chatRoomInfo, throwable))
                )
                .subscribe()));
  }

  Flux<ChatRoomInfo> writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux);
  Flux<ChatRoomInfo> readChatRoomInfo();
  Flux<Message> writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux);
  Flux<Message> readChatRoomData(UUID chatRoomId);
}
