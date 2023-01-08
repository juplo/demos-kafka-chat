package de.juplo.kafka.chat.backend.domain;

import java.util.UUID;


public interface ChatroomFactory<Strategy extends PersistenceStrategy>
{
  Chatroom createChatroom(UUID id, String name);
}
