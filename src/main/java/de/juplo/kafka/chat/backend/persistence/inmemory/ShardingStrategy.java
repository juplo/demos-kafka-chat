package de.juplo.kafka.chat.backend.domain;

import java.util.UUID;


public interface ShardingStrategy
{
  int selectShard(UUID chatRoomId);
}
