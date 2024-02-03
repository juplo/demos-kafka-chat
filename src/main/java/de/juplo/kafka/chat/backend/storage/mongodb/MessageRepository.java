package de.juplo.kafka.chat.backend.storage.mongodb;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;


public interface MessageRepository extends ReactiveMongoRepository<MessageTo, String>
{
  Flux<MessageTo> findByChatRoomIdOrderBySerialAsc(String chatRoomId);
}
