package de.juplo.kafka.chat.backend.domain;

import reactor.core.publisher.Mono;


public interface ShardingPublisherStrategy
{
  Mono<String> publishOwnership(int shard);
}
