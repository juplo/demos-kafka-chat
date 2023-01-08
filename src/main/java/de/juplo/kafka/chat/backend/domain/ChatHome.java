package de.juplo.kafka.chat.backend.domain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


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

  public Mono<ChatRoom> createChatroom(String name)
  {
    ChatRoom chatroom = service.createChatroom(UUID.randomUUID(), name);
    chatrooms.put(chatroom.getId(), chatroom);
    return Mono.just(chatroom);
  }

  public Mono<ChatRoom> getChatroom(UUID id)
  {
    ChatRoom chatroom = chatrooms.get(id);
    return chatroom == null
        ? Mono.error(() -> new UnknownChatroomException(id))
        : Mono.just(chatroom);
  }

  public Flux<ChatRoom> list()
  {
    return Flux.fromStream(chatrooms.values().stream());
  }
}
