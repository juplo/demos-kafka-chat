package de.juplo.kafka.chat.backend.persistence;

import java.util.UUID;


public interface ShardingStrategy
{
  int selectShard(UUID chatRoomId);
}
