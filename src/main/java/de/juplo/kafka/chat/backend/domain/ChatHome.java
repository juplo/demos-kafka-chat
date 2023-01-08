package de.juplo.kafka.chat.backend.domain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Stream;


@Slf4j
public class ChatHome
{
  private final Map<UUID, Chatroom> chatrooms;
  private final ChatHomeService service;

  public ChatHome(ChatHomeService service, Flux<Chatroom> chatroomFlux)
  {
    log.debug("Creating ChatHome with factory: {}", service);
    this.service = service;
    this.chatrooms = new HashMap<>();
    chatroomFlux.subscribe(chatroom -> chatrooms.put(chatroom.getId(), chatroom));
  }

  public Chatroom createChatroom(String name)
  {
    Chatroom chatroom = service.createChatroom(UUID.randomUUID(), name);
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
