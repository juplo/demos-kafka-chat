package de.juplo.kafka.chat.backend.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;


public interface ChatroomService
{
  Mono<Message> persistMessage(
      Message.MessageKey key,
      LocalDateTime timestamp,
      String text);

  Mono<Message> getMessage(Message.MessageKey key);

  Flux<Message> getMessages(long first, long last);
}
