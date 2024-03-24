package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatMessageService;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;


@Slf4j
public class InMemoryChatMessageService implements ChatMessageService
{
  @Getter
  private final ChatRoomInfo chatRoomInfo;
  private final LinkedHashMap<Message.MessageKey, Message> messages;


  public InMemoryChatMessageService(ChatRoomInfo chatRoomInfo)
  {
    log.debug("Creating InMemoryChatMessageService");
    this.chatRoomInfo = chatRoomInfo;
    messages = new LinkedHashMap<>();
  }


  Mono<Void> restore(StorageStrategy storageStrategy)
  {
    Flux<Message> messageFlux = storageStrategy.readChatRoomData(chatRoomInfo.getId());

    return messageFlux
        .doOnNext(message -> messages.put(message.getKey(), message))
        .count()
        .doOnSuccess(count -> log.info("Restored InMemoryChatMessageService with {} messages", count))
        .doOnError(throwable -> log.error("Could not restore InMemoryChatMessageService"))
        .then();
  }

  @Override
  public Mono<Message> persistMessage(
      Message.MessageKey key,
      LocalDateTime timestamp,
      String text)
  {
    Message message = new Message(key, (long)messages.size(), timestamp, text);
    messages.put(message.getKey(), message);
    return Mono.just(message);
  }

  @Override
  public Mono<Message> getMessage(Message.MessageKey key)
  {
    return Mono.fromSupplier(() -> messages.get(key));
  }

  @Override
  public Flux<Message> getMessages(long first, long last)
  {
    return Flux.fromStream(messages
        .values()
        .stream()
        .filter(message ->
        {
          long serial = message.getSerialNumber();
          return serial >= first && serial <= last;
        }));
  }
}
