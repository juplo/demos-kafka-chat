package de.juplo.kafka.chat.backend.domain;

import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Stream;


@RequiredArgsConstructor
public class ChatHome
{
  private final Map<UUID, Chatroom> chatrooms;
  private final ChatroomFactory factory;


  public Chatroom createChatroom(String name)
  {
    Chatroom chatroom = factory.createChatroom(UUID.randomUUID(), name);
    chatrooms.put(chatroom.getId(), chatroom);
    return chatroom;
  }

  public Optional<Chatroom> getChatroom(UUID id)
  {
    return Optional.ofNullable(chatrooms.get(id));
  }

  public Stream<Chatroom> list()
  {
    return chatrooms.values().stream();
  }
}