package de.juplo.kafka.chat.backend.persistence.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.Message;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.UUID;


@AllArgsConstructor
@NoArgsConstructor
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@EqualsAndHashCode(of = { "chatRoomId", "user", "id" })
@ToString(of = { "chatRoomId", "user", "id" })
@Document
class MessageTo
{
  @Indexed
  private String chatRoomId;
  @Indexed
  private String user;
  @Field("id")
  @Indexed
  private Long id;
  @Indexed
  private Long serial;
  private String time;
  private String text;

  Message toMessage()
  {
    return new Message(
        Message.MessageKey.of(user, id),
        serial,
        LocalDateTime.parse(time),
        text);
  }

  static MessageTo from(UUID chatRoomId, Message message)
  {
    return
        new MessageTo(
            chatRoomId.toString(),
            message.getUsername(),
            message.getId(),
            message.getSerialNumber(),
            message.getTimestamp().toString(),
            message.getMessageText());
  }
}
