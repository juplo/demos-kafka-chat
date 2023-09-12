package de.juplo.kafka.chat.backend.implementation.kafka.messages;


import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class AbstractMessageTo
{
  public enum ToType {
    COMMAND_CREATE_CHATROOM,
    EVENT_CHATMESSAGE_RECEIVED,
    EVENT_CHATROOM_CREATED,
  }

  @Getter
  private final ToType type;
}
