package de.juplo.kafka.chat.backend.api;

import java.util.UUID;


public interface ShardingStrategy
{
  int selectShard(UUID chatRoomId);
}
