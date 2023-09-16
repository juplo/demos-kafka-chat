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
    EVENT_SHARD_ASSIGNED,
    EVENT_SHARD_REVOKED,
  }

  @Getter
  private final ToType type;
}
