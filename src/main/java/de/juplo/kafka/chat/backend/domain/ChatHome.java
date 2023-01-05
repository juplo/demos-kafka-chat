package de.juplo.kafka.chat.backend.domain;

import lombok.RequiredArgsConstructor;

import java.util.*;


@RequiredArgsConstructor
public class ChatHome
{
  private final Map<UUID, Chatroom> chatrooms = new HashMap<>();
  private final ChatroomFactory factory;


  public Chatroom createChatroom(String name)
  {
    Chatroom chatroom = factory.createChatroom(UUID.randomUUID(), name);
    chatrooms.put(chatroom.getId(), chatroom);
    return chatroom;
  }

  public Chatroom getChatroom(UUID id)
  {
    return chatrooms.get(id);
  }

  public Collection<Chatroom> list()
  {
    return chatrooms.values();
  }
}
