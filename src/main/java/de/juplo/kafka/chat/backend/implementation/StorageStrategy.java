package de.juplo.kafka.chat.backend.implementation;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface StorageStrategy
{
  Logger log = LoggerFactory.getLogger(StorageStrategy.class.getCanonicalName());

  default Mono<Void> write(ChatHomeService chatHomeService)
  {
    return writeChatRoomInfo(chatHomeService.getChatRoomInfo())
        .flatMap(chatRoomInfo -> writeChatRoomData(
            chatRoomInfo.getId(),
            chatHomeService
                .getChatRoomData(chatRoomInfo.getId())
                .flatMapMany(chatRoomData -> chatRoomData.getMessages())
            )
            .count()
            .doOnSuccess(count -> log.info("Stored {} messages for {}", count, chatRoomInfo))
            .doOnError(throwable -> log.error("Could not store {}: {}", chatRoomInfo, throwable)))
        .count()
        .doOnSuccess(count -> log.info("Stored {} chat-rooms for {}", count, chatHomeService))
        .doOnError(throwable -> log.error("Could not store {}: {}", chatHomeService, throwable))
        .then();
  }

  Flux<ChatRoomInfo> writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux);
  Flux<ChatRoomInfo> readChatRoomInfo();
  Flux<Message> writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux);
  Flux<Message> readChatRoomData(UUID chatRoomId);
}
