package de.juplo.kafka.chat.backend.storage.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface MessageRepository extends MongoRepository<MessageTo, String>
{
  List<MessageTo> findByChatRoomIdOrderBySerialAsc(String chatRoomId);
}
