package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.persistence.ShardingStrategy;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.utils.Utils;

import java.util.UUID;


@RequiredArgsConstructor
public class KafkaLikeShardingStrategy implements ShardingStrategy
{
  private final int numPartitions;

  @Override
  public int selectShard(UUID chatRoomId)
  {
    byte[] serializedKey = chatRoomId.toString().getBytes();
    return Utils.toPositive(Utils.murmur2(serializedKey)) % numPartitions;
  }
}
