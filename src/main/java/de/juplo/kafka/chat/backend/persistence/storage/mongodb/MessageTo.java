package de.juplo.kafka.chat.backend.persistence.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.Message;
import lombok.*;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@AllArgsConstructor
@NoArgsConstructor
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@EqualsAndHashCode(of = { "user", "id" })
@ToString(of = { "user", "id" })
class MessageTo
{
  final static Pattern SPLIT_ID = Pattern.compile("^([a-z-0-9]+)--([0-9]+)$");
  private String id;
  private Long serial;
  private String time;
  private String text;

  Message toMessage()
  {
    Matcher matcher = SPLIT_ID.matcher(id);
    if (!matcher.matches())
      throw new RuntimeException("MessageTo with invalid ID: " + id);
    Long messageId = Long.parseLong(matcher.group(2));
    String user = matcher.group(1);
    return new Message(
        Message.MessageKey.of(user, messageId),
        serial,
        LocalDateTime.parse(time),
        text);
  }

  static MessageTo from(Message message)
  {
    return
        new MessageTo(
             message.getUsername() + "--" + message.getId(),
            message.getSerialNumber(),
            message.getTimestamp().toString(),
            message.getMessageText());
  }
}
