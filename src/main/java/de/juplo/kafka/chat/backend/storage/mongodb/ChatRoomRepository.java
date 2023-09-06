package de.juplo.kafka.chat.backend.storage.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;


public interface ChatRoomRepository extends MongoRepository<ChatRoomTo, String>
{
}
