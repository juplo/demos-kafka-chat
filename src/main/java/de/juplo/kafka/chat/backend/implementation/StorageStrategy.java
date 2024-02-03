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

  default void write(ChatHomeService chatHomeService)
  {
    writeChatRoomInfo(
        chatHomeService
            .getChatRoomInfo()
            .doOnNext(chatRoomInfo -> writeChatRoomData(
                chatRoomInfo.getId(),
                chatHomeService
                    .getChatRoomData(chatRoomInfo.getId())
                    .flatMapMany(chatRoomData -> chatRoomData.getMessages()),
                this::logSuccessChatRoom,
                this::logFailureChatRoom)));
  }

  void writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux);
  Flux<ChatRoomInfo> readChatRoomInfo();
  default void writeChatRoomData(
      UUID chatRoomId,
      Flux<Message> messageFlux,
      ChatRoomWrittenSuccessCallback successCallback,
      ChatRoomWrittenFailureCallback failureCallback)
  {
    writeChatRoomData(
        chatRoomId,
        messageFlux
            .doOnComplete(() -> successCallback.accept(chatRoomId))
            .doOnError(throwable -> failureCallback.accept(chatRoomId, throwable)));
  }
  void writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux);
  Flux<Message> readChatRoomData(UUID chatRoomId);

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
