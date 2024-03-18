package de.juplo.kafka.chat.backend.implementation.kafka.messages.data;

import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import lombok.*;


@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
public class EventChatMessageReceivedTo extends AbstractMessageTo
{
  private String user;
  private Long id;
  private String text;


  public EventChatMessageReceivedTo()
  {
    super(ToType.EVENT_CHATMESSAGE_RECEIVED);
  }


  public static EventChatMessageReceivedTo of(String user, Long id, String text)
  {
    EventChatMessageReceivedTo to = new EventChatMessageReceivedTo();
    to.user = user;
    to.id = id;
    to.text = text;
    return to;
  }
}
