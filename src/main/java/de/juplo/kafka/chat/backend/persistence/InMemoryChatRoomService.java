package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.domain.MessageMutationException;
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
    log.debug("Creating InMemoryChatroomService");
    messages = new LinkedHashMap<>();
    messageFlux.subscribe(message -> persistMessage(message));
  }

  @Override
  public Mono<Message> persistMessage(
      Message.MessageKey key,
      LocalDateTime timestamp,
      String text)
  {
    Message message = new Message(key, (long)messages.size(), timestamp, text);
    return Mono.justOrEmpty(persistMessage(message));
  }

  private Message persistMessage(Message message)
  {
    Message.MessageKey key = message.getKey();
    Message existing = messages.get(key);
    if (existing != null)
    {
      log.info("Message with key {} already exists; {}", key, existing);
      if (!message.equals(existing))
        throw new MessageMutationException(message, existing);
      return null;
    }

    messages.put(key, message);
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
