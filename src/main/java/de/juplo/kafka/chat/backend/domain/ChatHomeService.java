package de.juplo.kafka.chat.backend.domain;

import java.util.UUID;


public interface ChatHomeService<T extends ChatRoomService>
{
  ChatRoom createChatroom(UUID id, String name);
}
