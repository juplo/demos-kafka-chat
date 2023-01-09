package de.juplo.kafka.chat.backend.persistence.storage.files;

import de.juplo.kafka.chat.backend.domain.ChatRoomService;
import de.juplo.kafka.chat.backend.domain.Message;
import reactor.core.publisher.Flux;


public interface ChatRoomServiceFactory
{
  ChatRoomService create(Flux<Message> messageFlux);
}
