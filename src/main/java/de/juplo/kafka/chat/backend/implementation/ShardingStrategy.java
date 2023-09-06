package de.juplo.kafka.chat.backend.implementation;

import java.util.UUID;


public interface ShardingStrategy
{
  int selectShard(UUID chatRoomId);
}
