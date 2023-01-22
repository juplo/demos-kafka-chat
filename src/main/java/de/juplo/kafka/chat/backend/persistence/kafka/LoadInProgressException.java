package de.juplo.kafka.chat.backend.persistence.kafka;

import de.juplo.kafka.chat.backend.domain.ShardNotOwnedException;


public class LoadInProgressException extends ShardNotOwnedException
{
  public LoadInProgressException()
  {
    this(-1);
  }

  public LoadInProgressException(int shard)
  {
    super(shard);
  }
}
