package de.juplo.kafka.chat.backend.domain;

import java.util.UUID;


public interface ChatroomFactory<T extends ChatroomService>
{
  Chatroom createChatroom(UUID id, String name);
}
