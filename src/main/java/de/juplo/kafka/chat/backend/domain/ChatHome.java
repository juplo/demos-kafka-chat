package de.juplo.kafka.chat.backend.domain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Stream;


@Slf4j
public class ChatHome
{
  private final Map<UUID, ChatRoom> chatrooms;
  private final ChatHomeService service;

  public ChatHome(ChatHomeService service, Flux<ChatRoom> chatroomFlux)
  {
    log.debug("Creating ChatHome with factory: {}", service);
    this.service = service;
    this.chatrooms = new HashMap<>();
    chatroomFlux.subscribe(chatroom -> chatrooms.put(chatroom.getId(), chatroom));
  }

  public ChatRoom createChatroom(String name)
  {
    ChatRoom chatroom = service.createChatroom(UUID.randomUUID(), name);
    chatrooms.put(chatroom.getId(), chatroom);
    return chatroom;
  }

  public Optional<ChatRoom> getChatroom(UUID id)
  {
    return Optional.ofNullable(chatrooms.get(id));
  }

  public Stream<ChatRoom> list()
  {
    return chatrooms.values().stream();
  }
}
