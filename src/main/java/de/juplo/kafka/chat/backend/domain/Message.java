package de.juplo.kafka.chat.backend.domain;

import lombok.*;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class Message
{
  private final MessageKey key;
  private final Long serialNumber;
  private final LocalDateTime timestamp;
  private final String messageText;

  public Long getId()
  {
    return key.messageId;
  }

  public String getUsername()
  {
    return key.username;
  }


  @Value(staticConstructor = "of")
  public static class MessageKey
  {
    String username;
    Long messageId;
  }
}
