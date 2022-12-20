package de.juplo.kafka.chatroom.api;

import de.juplo.kafka.chatroom.domain.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@AllArgsConstructor
public class MessageTo
{
  private UUID id;
  private Long serialNumber;
  private LocalDateTime timestamp;
  private String user;
  private String text;

  public static MessageTo from(Message message)
  {
    return
        new MessageTo(
            message.getId(),
            message.getSerialNumber(),
            message.getTimestamp(),
            message.getUser(),
            message.getText());
  }
}
