package de.juplo.kafka.chat.backend.domain;

import java.util.UUID;


public interface ChatHomeService<T extends ChatroomService>
{
  ChatRoom createChatroom(UUID id, String name);
}
