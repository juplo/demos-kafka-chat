package de.juplo.kafka.chat.backend.persistence.kafka.messages;

import de.juplo.kafka.chat.backend.domain.Message;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@EqualsAndHashCode
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


  public Message toMessage(long offset, LocalDateTime timestamp)
  {
    return new Message(Message.MessageKey.of(user, id), offset, timestamp, text);
  }

  public static EventChatMessageReceivedTo from(Message message)
  {
    return EventChatMessageReceivedTo.of(
        message.getUsername(),
        message.getId(),
        message.getMessageText());
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
