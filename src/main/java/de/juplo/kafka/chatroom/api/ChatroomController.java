package de.juplo.kafka.chatroom.api;

import de.juplo.kafka.chatroom.domain.Chatroom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
public class ChatroomController
{
  private final Map<UUID, Chatroom> chatrooms = new HashMap<>();
  private final Clock clock;


  @PostMapping("create")
  public Chatroom create(@RequestBody String name)
  {
    Chatroom chatroom = new Chatroom(UUID.randomUUID(), name);
    chatrooms.put(chatroom.getId(), chatroom);
    return chatroom;
  }

  @GetMapping("list")
  public Collection<Chatroom> list()
  {
    return chatrooms.values();
  }

  @GetMapping("get/{chatroomId}")
  public Chatroom get(@PathVariable UUID chatroomId)
  {
    return chatrooms.get(chatroomId);
  }

  @PutMapping("post/{chatroomId}/{username}/{messageId}")
  public Mono<MessageTo> post(
      @PathVariable UUID chatroomId,
      @PathVariable String username,
      @PathVariable UUID messageId,
      @RequestBody String text)
  {
    return
        chatrooms
            .get(chatroomId)
            .addMessage(
                messageId,
                LocalDateTime.now(clock),
                username,
                text)
            .map(message -> MessageTo.from(message));
  }

  @GetMapping(
      path = "listen/{chatroomId}",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<MessageTo> listen(@PathVariable UUID chatroomId)
  {
    return chatrooms
        .get(chatroomId)
        .listen()
        .map(message -> MessageTo.from(message));
  }
}
