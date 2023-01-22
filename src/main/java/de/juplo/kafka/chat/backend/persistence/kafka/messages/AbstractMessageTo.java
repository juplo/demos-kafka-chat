package de.juplo.kafka.chat.backend.persistence.kafka.messages;


import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class AbstractMessageTo
{
  public enum ToType {
    COMMAND_CREATE_CHATROOM,
    EVENT_CHATMESSAGE_RECEIVED,
  }

  @Getter
  private final ToType type;
}
