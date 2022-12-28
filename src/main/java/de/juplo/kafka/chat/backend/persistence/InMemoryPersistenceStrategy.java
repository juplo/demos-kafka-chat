package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.domain.MessageMutationException;
import de.juplo.kafka.chat.backend.domain.PersistenceStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;


@RequiredArgsConstructor
@Slf4j
public class InMemoryPersistenceStrategy implements PersistenceStrategy
{
  private final LinkedHashMap<Message.MessageKey, Message> messages = new LinkedHashMap<>();

  @Override
  public Mono<Message> persistMessage(
      Message.MessageKey key,
      LocalDateTime timestamp,
      String text)
  {
    Message message = new Message(key, (long)messages.size(), timestamp, text);

    Message existing = messages.get(key);
    if (existing != null)
    {
      log.info("Message with key {} already exists; {}", key, existing);
      if (!message.equals(existing))
        throw new MessageMutationException(message, existing);
      return Mono.empty();
    }

    messages.put(key, message);
    return Mono
        .fromSupplier(() -> message)
        .log();
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
