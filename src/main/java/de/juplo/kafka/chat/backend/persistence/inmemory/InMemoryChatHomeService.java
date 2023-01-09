package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


@Slf4j
public class InMemoryChatHomeService implements ChatHomeService<InMemoryChatRoomService>
{
  private final Map<UUID, ChatRoom> chatrooms;
  private final Clock clock;
  private final int bufferSize;


  public InMemoryChatHomeService(
      Flux<ChatRoom> chatroomFlux,
      Clock clock,
      int bufferSize)
  {
    log.debug("Creating InMemoryChatHomeService with buffer-size {} (for created ChatRoom's)", bufferSize);
    this.chatrooms = new HashMap<>();
    chatroomFlux.subscribe(chatroom -> chatrooms.put(chatroom.getId(), chatroom));
    this.clock = clock;
    this.bufferSize = bufferSize;
  }

  @Override
  public Mono<ChatRoom> createChatRoom(UUID id, String name)
  {
    InMemoryChatRoomService service =
        new InMemoryChatRoomService(new LinkedHashMap<>());
    ChatRoom chatRoom = new ChatRoom(id, name, clock, service, bufferSize);
    chatrooms.put(chatRoom.getId(), chatRoom);
    return Mono.just(chatRoom);
  }

  @Override
  public Mono<ChatRoom> getChatRoom(UUID id)
  {
    return Mono.justOrEmpty(chatrooms.get(id));
  }

  @Override
  public Flux<ChatRoom> getChatRooms()
  {
    return Flux.fromStream(chatrooms.values().stream());
  }
}
