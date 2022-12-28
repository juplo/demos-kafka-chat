package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
public class MessageTo
{
  private Long id;
  private Long serial;
  private LocalDateTime time;
  private String user;
  private String text;

  public static MessageTo from(Message message)
  {
    return
        new MessageTo(
            message.getId(),
            message.getSerialNumber(),
            message.getTimestamp(),
            message.getUsername(),
            message.getMessageText());
  }
}