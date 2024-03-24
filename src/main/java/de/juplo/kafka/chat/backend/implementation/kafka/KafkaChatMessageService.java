package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.domain.ChatMessageService;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;


@RequiredArgsConstructor
@Slf4j
public class KafkaChatMessageService implements ChatMessageService
{
  private final DataChannel dataChannel;
  @Getter
  private final ChatRoomInfo chatRoomInfo;

  private final LinkedHashMap<Message.MessageKey, Message> messages = new LinkedHashMap<>();


  @Override
  public Mono<Message> persistMessage(
    Message.MessageKey key,
    LocalDateTime timestamp,
    String text)
  {
    return dataChannel
        .sendChatMessage(chatRoomInfo.getId(), key, timestamp, text)
        .doOnSuccess(message -> persistMessage(message));
  }

  void persistMessage(Message message)
  {
    messages.put  (message.getKey(), message);
  }

  @Override
  synchronized public Mono<Message> getMessage(Message.MessageKey key)
  {
    return Mono.fromSupplier(() -> messages.get(key));
  }

  @Override
  synchronized public Flux<Message> getMessages(long first, long last)
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
