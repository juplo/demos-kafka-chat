package de.juplo.kafka.chat.backend.implementation.kafka.messages.info;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;


@Getter
@Setter
@EqualsAndHashCode
@ToString
public class EventChatRoomCreated extends AbstractMessageTo
{
  private String id;
  private String name;
  private Integer shard;


  public EventChatRoomCreated()
  {
    super(ToType.EVENT_CHATROOM_CREATED);
  }


  public ChatRoomInfo toChatRoomInfo()
  {
    return new ChatRoomInfo(UUID.fromString(id), name, shard);
  }

  public static EventChatRoomCreated of(UUID id, String name, Integer shard)
  {
    EventChatRoomCreated event = new EventChatRoomCreated();

    event.setId(id.toString());
    event.setName(name);
    event.setShard(shard);

    return event;
  }
}
