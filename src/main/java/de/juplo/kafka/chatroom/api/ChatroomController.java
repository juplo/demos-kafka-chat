package de.juplo.kafka.chatroom.api;

import de.juplo.kafka.chatroom.domain.Chatroom;
import de.juplo.kafka.chatroom.domain.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDateTime;
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

  @PutMapping("post/{chatroomId}/{username}/{messageId}")
  public MessageTo post(
      @PathVariable UUID chatroomId,
      @PathVariable String username,
      @PathVariable UUID messageId,
      @RequestBody String message)
  {
    return MessageTo.from(
        chatrooms
            .get(chatroomId)
            .addMessage(
                messageId,
                LocalDateTime.now(clock),
                username,
                message));
  }
}
