package de.juplo.kafka.chat.backend.persistence.inmemory;

import java.util.UUID;


public interface ShardingStrategy
{
  int selectShard(UUID chatRoomId);
}
