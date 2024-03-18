package de.juplo.kafka.chat.backend.implementation.kafka.messages.info;

import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
public class EventShardAssigned extends AbstractMessageTo
{
  private int shard;
  private String uri;


  public EventShardAssigned()
  {
    super(ToType.EVENT_SHARD_ASSIGNED);
  }


  public static EventShardAssigned of(
      int shard,
      String uri)
  {
    EventShardAssigned event = new EventShardAssigned();
    event.setShard(shard);
    event.setUri(uri);
    return event;
  }
}
