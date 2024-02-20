package de.juplo.kafka.chat.backend.domain.exceptions;

import lombok.Getter;


public class ShardNotOwnedException extends IllegalStateException
{
  @Getter
  private final String instanceId;
  @Getter
  private final int shard;


  public ShardNotOwnedException(String instanceId, int shard)
  {
    super("Instance " + instanceId + " does not own the shard " + shard);
    this.instanceId = instanceId;
    this.shard = shard;
  }
}
