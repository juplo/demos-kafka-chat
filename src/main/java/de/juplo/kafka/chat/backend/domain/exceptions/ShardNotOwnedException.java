package de.juplo.kafka.chat.backend.domain.exceptions;

import lombok.Getter;


public class ShardNotOwnedException extends IllegalStateException
{
  @Getter
  private final int shard;


  public ShardNotOwnedException(int shard)
  {
    super("This instance does not own the shard " + shard);
    this.shard = shard;
  }
}
