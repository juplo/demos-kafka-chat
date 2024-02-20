package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatMessageService;
import de.juplo.kafka.chat.backend.domain.Message;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;


@Slf4j
public class InMemoryChatMessageService implements ChatMessageService
{
  private final LinkedHashMap<Message.MessageKey, Message> messages;


  public InMemoryChatMessageService(Flux<Message> messageFlux)
  {
    log.debug("Creating InMemoryChatMessageService");
    messages = new LinkedHashMap<>();
    messageFlux
        .doOnNext(message -> messages.put(message.getKey(), message))
        .then()
        .doOnSuccess(empty -> log.info("Restored InMemoryChatMessageService"))
        .doOnError(throwable -> log.error("Could not restore InMemoryChatMessageService"))
        .block();
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
