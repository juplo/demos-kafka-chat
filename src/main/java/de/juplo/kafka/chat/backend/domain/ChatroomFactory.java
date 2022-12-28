package de.juplo.kafka.chat.backend.domain;

import java.util.UUID;


public interface ChatroomFactory
{
  Chatroom createChatroom(UUID id, String name);
}
