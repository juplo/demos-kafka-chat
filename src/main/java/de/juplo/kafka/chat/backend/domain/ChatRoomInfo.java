package de.juplo.kafka.chat.backend.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.UUID;


@RequiredArgsConstructor
@EqualsAndHashCode(of = { "id" })
@ToString(of = { "id", "name", "shard" })
public class ChatRoomInfo
{
  @Getter
  private final UUID id;
  @Getter
  private final String name;
  @Getter
  private final Integer shard;
}
