package de.juplo.kafka.chatroom.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;


@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class Message
{
  private final UUID id;
  private final Long serialNumber;
  private final LocalDateTime timestamp;
  private final String user;
  private final String text;
}
