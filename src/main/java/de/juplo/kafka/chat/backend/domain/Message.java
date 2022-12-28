package de.juplo.kafka.chat.backend.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class Message
{
  private final Long id;
  private final Long serialNumber;
  private final LocalDateTime timestamp;
  private final String username;
  private final String messageText;
}
