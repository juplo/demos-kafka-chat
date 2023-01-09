package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.domain.ChatRoomService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;


@Slf4j
public class InMemoryChatRoomService implements ChatRoomService
{
  private final LinkedHashMap<Message.MessageKey, Message> messages;


  public InMemoryChatRoomService(LinkedHashMap<Message.MessageKey, Message> messages)
  {
    this.messages = messages;
  }

  public InMemoryChatRoomService(Flux<Message> messageFlux)
  {
    log.debug("Creating InMemoryChatRoomService");
    messages = new LinkedHashMap<>();
    messageFlux.subscribe(message -> messages.put(message.getKey(), message));
  }

  @Override
  public Message persistMessage(
      Message.MessageKey key,
      LocalDateTime timestamp,
      String text)
  {
    Message message = new Message(key, (long)messages.size(), timestamp, text);
    messages.put(message.getKey(), message);
    return message;
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
