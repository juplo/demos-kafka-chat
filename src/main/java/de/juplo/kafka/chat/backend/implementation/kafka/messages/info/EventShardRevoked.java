package de.juplo.kafka.chat.backend.implementation.kafka.messages.info;

import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@EqualsAndHashCode
@ToString
public class EventShardRevoked extends AbstractMessageTo
{
  private int shard;
  private String uri;


  public EventShardRevoked()
  {
    super(ToType.EVENT_SHARD_REVOKED);
  }


  public static EventShardRevoked of(
      int shard,
      String uri)
  {
    EventShardRevoked event = new EventShardRevoked();
    event.setShard(shard);
    event.setUri(uri);
    return event;
  }
}
