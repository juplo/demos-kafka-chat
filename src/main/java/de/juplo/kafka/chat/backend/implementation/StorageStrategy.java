package de.juplo.kafka.chat.backend.implementation;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public interface StorageStrategy
{
  Logger log = LoggerFactory.getLogger(StorageStrategy.class.getCanonicalName());

  default Flux<ChatRoomInfo> write(ChatHomeService chatHomeService)
  {
    return write(
        chatHomeService,
        this::logSuccessChatHomeService,
        this::logFailureChatHomeService);
  }

  default Flux<ChatRoomInfo> write(
      ChatHomeService chatHomeService,
      ChatHomeServiceWrittenSuccessCallback successCallback,
      ChatHomeServiceWrittenFailureCallback failureCallback)
  {
    return writeChatRoomInfo(
        chatHomeService
            .getChatRoomInfo()
            .doOnComplete(() -> successCallback.accept(chatHomeService))
            .doOnError(throwable -> failureCallback.accept(chatHomeService, throwable))
            .doOnNext(chatRoomInfo -> writeChatRoomData(
                chatRoomInfo.getId(),
                chatHomeService
                    .getChatRoomData(chatRoomInfo.getId())
                    .flatMapMany(chatRoomData -> chatRoomData.getMessages()),
                this::logSuccessChatRoom,
                this::logFailureChatRoom
                )
                .subscribe()));
  }

  Flux<ChatRoomInfo> writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux);
  Flux<ChatRoomInfo> readChatRoomInfo();
  default Flux<Message> writeChatRoomData(
      UUID chatRoomId,
      Flux<Message> messageFlux,
      ChatRoomWrittenSuccessCallback successCallback,
      ChatRoomWrittenFailureCallback failureCallback)
  {
    return writeChatRoomData(
        chatRoomId,
        messageFlux
            .doOnComplete(() -> successCallback.accept(chatRoomId))
            .doOnError(throwable -> failureCallback.accept(chatRoomId, throwable)));
  }
  Flux<Message> writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux);
  Flux<Message> readChatRoomData(UUID chatRoomId);

  interface ChatHomeServiceWrittenSuccessCallback extends Consumer<ChatHomeService> {}
  interface ChatHomeServiceWrittenFailureCallback extends BiConsumer<ChatHomeService, Throwable> {}

  default void logSuccessChatHomeService(ChatHomeService chatHomeService)
  {
    log.info("Successfully stored {}", chatHomeService);
  }

  default void logFailureChatHomeService(ChatHomeService chatHomeService, Throwable throwable)
  {
    log.error("Could not store {}: {}", chatHomeService, throwable);
  }

  interface ChatRoomWrittenSuccessCallback extends Consumer<UUID> {}
  interface ChatRoomWrittenFailureCallback extends BiConsumer<UUID, Throwable> {}

  default void logSuccessChatRoom(UUID chatRoomId)
  {
    log.info("Successfully stored chat-room {}", chatRoomId);
  }

  default void logFailureChatRoom(UUID chatRoomId, Throwable throwable)
  {
    log.error("Could not store chat-room {}: {}", chatRoomId, throwable);
  }
}
