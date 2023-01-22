package de.juplo.kafka.chat.backend.persistence.kafka;

import de.juplo.kafka.chat.backend.domain.ChatRoomService;
import de.juplo.kafka.chat.backend.domain.Message;import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;


@RequiredArgsConstructor
@Slf4j
public class KafkaChatRoomService implements ChatRoomService
{
  private final ChatRoomChannel chatRoomChannel;
  private final UUID chatRoomId;

  private final LinkedHashMap<Message.MessageKey, Message> messages = new LinkedHashMap<>();


  @Override
  public Mono<Message> persistMessage(
    Message.MessageKey key,
    LocalDateTime timestamp,
    String text)
  {
    return chatRoomChannel
        .sendChatMessage(chatRoomId, key, timestamp, text)
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
