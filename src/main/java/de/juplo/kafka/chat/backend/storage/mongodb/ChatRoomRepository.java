package de.juplo.kafka.chat.backend.storage.mongodb;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;


public interface ChatRoomRepository extends ReactiveMongoRepository<ChatRoomTo, String>
{
}
