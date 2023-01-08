package de.juplo.kafka.chat.backend.domain;

import java.util.UUID;


public interface ChatHomeService<T extends ChatroomService>
{
  Chatroom createChatroom(UUID id, String name);
}
